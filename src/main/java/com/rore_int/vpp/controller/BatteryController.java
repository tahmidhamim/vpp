package com.rore_int.vpp.controller;

import com.rore_int.vpp.dto.BatterySearchResponse;
import com.rore_int.vpp.entity.Battery;
import com.rore_int.vpp.service.BatteryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/batteries")
@Validated
public class BatteryController {
    private static final Logger logger = LoggerFactory.getLogger(BatteryController.class);
    private final BatteryService batteryService;

    public BatteryController(BatteryService batteryService) {
        this.batteryService = batteryService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<List<Battery>>> createBatteries(@RequestBody @Valid List<Battery> batteries) {
        logger.info("Received request to create {} batteries", batteries.size());
        return batteryService.createBatteries(batteries)
                .thenApply(savedBatteries -> {
                    logger.info("Successfully processed creation of {} batteries", savedBatteries.size());
                    return ResponseEntity.ok(savedBatteries);
                })
                .exceptionally(throwable -> {
                    logger.error("Error processing battery creation: {}", throwable.getMessage(), throwable);
                    throw new RuntimeException("Failed to create batteries", throwable);
                });
    }

    @GetMapping("/search")
    public ResponseEntity<BatterySearchResponse> searchBatteriesByPostcodeRange(
            @RequestParam String minPostcode,
            @RequestParam String maxPostcode,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity) {
        logger.info("Received request to search batteries in postcode range: {} to {}, minCapacity: {}, maxCapacity: {}",
                minPostcode, maxPostcode, minCapacity, maxCapacity);
        BatterySearchResponse response = batteryService.searchBatteriesByPostcodeRange(minPostcode, maxPostcode, minCapacity, maxCapacity);
        return ResponseEntity.ok(response);
    }
}