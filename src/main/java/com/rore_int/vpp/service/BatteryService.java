package com.rore_int.vpp.service;

import com.rore_int.vpp.dto.BatterySearchResponse;
import com.rore_int.vpp.entity.Battery;
import com.rore_int.vpp.exception.DatabaseException;
import com.rore_int.vpp.exception.ValidationException;
import com.rore_int.vpp.repository.BatteryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class BatteryService {
    private static final Logger logger = LoggerFactory.getLogger(BatteryService.class);
    private final BatteryRepository batteryRepository;
    private final Executor taskExecutor;

    @Autowired
    public BatteryService(BatteryRepository batteryRepository, Executor taskExecutor) {
        this.batteryRepository = batteryRepository;
        this.taskExecutor = taskExecutor;
    }

    public CompletableFuture<List<Battery>> createBatteries(List<Battery> batteries) {
        logger.info("Creating {} batteries asynchronously", batteries.size());
        if (batteries == null || batteries.isEmpty()) {
            throw new ValidationException("Battery list cannot be null or empty");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Battery> savedBatteries = batteryRepository.saveAll(batteries);
                logger.info("Successfully created {} batteries", savedBatteries.size());
                return savedBatteries;
            } catch (DataAccessException e) {
                logger.error("Failed to save batteries: {}", e.getMessage(), e);
                throw new DatabaseException("Failed to save batteries", e);
            }
        }, taskExecutor);
    }

    public BatterySearchResponse searchBatteriesByPostcodeRange(String minPostcode, String maxPostcode,
                                                                Integer minCapacity, Integer maxCapacity) {
        logger.info("Searching batteries in postcode range: {} to {}, minCapacity: {}, maxCapacity: {}",
                minPostcode, maxPostcode, minCapacity, maxCapacity);
        validateSearchParameters(minPostcode, maxPostcode, minCapacity, maxCapacity);
        try {
            List<Battery> batteries = batteryRepository.findByPostcodeRangeAndCapacity(
                    minPostcode, maxPostcode, minCapacity, maxCapacity
            );

            List<String> batteryNames = batteries.stream()
                    .map(Battery::getName)
                    .collect(Collectors.toList());

            long totalCapacity = batteries.stream()
                    .mapToLong(Battery::getCapacity)
                    .sum();

            double averageCapacity = batteries.isEmpty() ? 0.0 :
                    batteries.stream()
                            .mapToDouble(Battery::getCapacity)
                            .average()
                            .orElse(0.0);

            BatterySearchResponse response = new BatterySearchResponse();
            response.setBatteryNames(batteryNames);
            response.setTotalCapacity(totalCapacity);
            response.setAverageCapacity(averageCapacity);

            logger.info("Found {} batteries in postcode range: {} to {}",
                    batteryNames.size(), minPostcode, maxPostcode);
            return response;
        } catch (DataAccessException ex) {
            logger.error("Failed to search batteries: {}", ex.getMessage(), ex);
            throw new DatabaseException("Failed to search batteries", ex);
        }
    }

    private void validateSearchParameters(String minPostcode, String maxPostcode, Integer minCapacity, Integer maxCapacity) {
        if (minPostcode == null || minPostcode.isBlank()) {
            throw new ValidationException("minPostcode is mandatory");
        }
        if (maxPostcode == null || maxPostcode.isBlank()) {
            throw new ValidationException("maxPostcode is mandatory");
        }
        if (!minPostcode.matches("\\d{4}")) {
            throw new ValidationException("minPostcode must be a 4-digit number");
        }
        if (!maxPostcode.matches("\\d{4}")) {
            throw new ValidationException("maxPostcode must be a 4-digit number");
        }
        if (minPostcode.compareTo(maxPostcode) > 0) {
            throw new ValidationException("minPostcode must not be greater than maxPostcode");
        }
        if (minCapacity != null && minCapacity < 1) {
            throw new ValidationException("minCapacity must be positive");
        }
        if (maxCapacity != null && maxCapacity < 1) {
            throw new ValidationException("maxCapacity must be positive");
        }
        if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
            throw new ValidationException("minCapacity must not be greater than maxCapacity");
        }
    }
}