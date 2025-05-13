package com.rore_int.vpp.service;

import com.rore_int.vpp.dto.BatterySearchResponse;
import com.rore_int.vpp.entity.Battery;
import com.rore_int.vpp.exception.DatabaseException;
import com.rore_int.vpp.exception.ValidationException;
import com.rore_int.vpp.repository.BatteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatteryServiceTest {

    @Mock
    private BatteryRepository batteryRepository;

    @Mock
    private Executor taskExecutor;

    @InjectMocks
    private BatteryService batteryService;

    private List<Battery> batteries;
    private String minPostcode;
    private String maxPostcode;
    private Integer minCapacity;
    private Integer maxCapacity;

    @BeforeEach
    void setUp() {
        Battery battery1 = new Battery();
        battery1.setName("Cannington");
        battery1.setPostcode("6107");
        battery1.setCapacity(13500);

        Battery battery2 = new Battery();
        battery2.setName("Midland");
        battery2.setPostcode("6057");
        battery2.setCapacity(50500);

        batteries = Arrays.asList(battery1, battery2);

        minPostcode = "6000";
        maxPostcode = "6200";
        minCapacity = 10000;
        maxCapacity = 60000;
    }

    @Test
    void createBatteries_shouldSaveAndReturnBatteriesAsync() throws Exception {
        // Stub taskExecutor to run tasks synchronously
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        when(batteryRepository.saveAll(anyList())).thenReturn(batteries);

        CompletableFuture<List<Battery>> future = batteryService.createBatteries(batteries);
        List<Battery> savedBatteries = future.get();

        assertNotNull(savedBatteries);
        assertEquals(2, savedBatteries.size());
        assertEquals("Cannington", savedBatteries.get(0).getName());
        assertEquals("Midland", savedBatteries.get(1).getName());
        verify(batteryRepository, times(1)).saveAll(batteries);
    }

    @Test
    void createBatteries_shouldThrowValidationExceptionForEmptyList() {
        assertThrows(ValidationException.class, () -> batteryService.createBatteries(Collections.emptyList()),
                "Battery list cannot be null or empty");
        verify(batteryRepository, never()).saveAll(anyList());
    }

    @Test
    void createBatteries_shouldThrowDatabaseExceptionOnDataAccessError() throws Exception {
        // Stub taskExecutor to run tasks synchronously
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        when(batteryRepository.saveAll(anyList())).thenThrow(new EmptyResultDataAccessException(1));

        CompletableFuture<List<Battery>> future = batteryService.createBatteries(batteries);
        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(DatabaseException.class, thrown.getCause());
        assertEquals("Failed to save batteries", thrown.getCause().getMessage());
        assertInstanceOf(EmptyResultDataAccessException.class, thrown.getCause().getCause());
        verify(batteryRepository, times(1)).saveAll(batteries);
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldReturnSortedNamesAndStatsWithCapacityFilters() {
        when(batteryRepository.findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, minCapacity, maxCapacity))
                .thenReturn(batteries);

        BatterySearchResponse response = batteryService.searchBatteriesByPostcodeRange(
                minPostcode, maxPostcode, minCapacity, maxCapacity);

        assertNotNull(response);
        assertEquals(Arrays.asList("Cannington", "Midland"), response.getBatteryNames());
        assertEquals(64000, response.getTotalCapacity());
        assertEquals(32000.0, response.getAverageCapacity(), 0.01);
        verify(batteryRepository, times(1)).findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, minCapacity, maxCapacity);
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldReturnEmptyWhenNoBatteriesFound() {
        when(batteryRepository.findByPostcodeRangeAndCapacity("7000", "7200", null, null))
                .thenReturn(Collections.emptyList());

        BatterySearchResponse response = batteryService.searchBatteriesByPostcodeRange("7000", "7200", null, null);

        assertNotNull(response);
        assertTrue(response.getBatteryNames().isEmpty());
        assertEquals(0, response.getTotalCapacity());
        assertEquals(0.0, response.getAverageCapacity(), 0.01);
        verify(batteryRepository, times(1)).findByPostcodeRangeAndCapacity("7000", "7200", null, null);
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldFilterByMinCapacityOnly() {
        List<Battery> filteredBatteries = Arrays.asList(batteries.get(1)); // Only Midland (50500)
        when(batteryRepository.findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, 50000, null))
                .thenReturn(filteredBatteries);

        BatterySearchResponse response = batteryService.searchBatteriesByPostcodeRange(minPostcode, maxPostcode, 50000, null);

        assertNotNull(response);
        assertEquals(Arrays.asList("Midland"), response.getBatteryNames());
        assertEquals(50500, response.getTotalCapacity());
        assertEquals(50500.0, response.getAverageCapacity(), 0.01);
        verify(batteryRepository, times(1)).findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, 50000, null);
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldFilterByMaxCapacityOnly() {
        List<Battery> filteredBatteries = Arrays.asList(batteries.get(0)); // Only Cannington (13500)
        when(batteryRepository.findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, null, 20000))
                .thenReturn(filteredBatteries);

        BatterySearchResponse response = batteryService.searchBatteriesByPostcodeRange(minPostcode, maxPostcode, null, 20000);

        assertNotNull(response);
        assertEquals(Arrays.asList("Cannington"), response.getBatteryNames());
        assertEquals(13500, response.getTotalCapacity());
        assertEquals(13500.0, response.getAverageCapacity(), 0.01);
        verify(batteryRepository, times(1)).findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, null, 20000);
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldThrowValidationExceptionForInvalidMinPostcode() {
        assertThrows(ValidationException.class,
                () -> batteryService.searchBatteriesByPostcodeRange("123", maxPostcode, minCapacity, maxCapacity),
                "minPostcode must be a 4-digit number");
        verify(batteryRepository, never()).findByPostcodeRangeAndCapacity(anyString(), anyString(), any(), any());
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldThrowValidationExceptionForInvalidMaxPostcode() {
        assertThrows(ValidationException.class,
                () -> batteryService.searchBatteriesByPostcodeRange(minPostcode, "123", minCapacity, maxCapacity),
                "maxPostcode must be a 4-digit number");
        verify(batteryRepository, never()).findByPostcodeRangeAndCapacity(anyString(), anyString(), any(), any());
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldThrowValidationExceptionForNegativeMinCapacity() {
        assertThrows(ValidationException.class,
                () -> batteryService.searchBatteriesByPostcodeRange(minPostcode, maxPostcode, -1, maxCapacity),
                "minCapacity must be positive");
        verify(batteryRepository, never()).findByPostcodeRangeAndCapacity(anyString(), anyString(), any(), any());
    }

    @Test
    void searchBatteriesByPostcodeRange_shouldThrowDatabaseExceptionOnDataAccessError() {
        when(batteryRepository.findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, minCapacity, maxCapacity))
                .thenThrow(new EmptyResultDataAccessException(1));

        DatabaseException thrown = assertThrows(DatabaseException.class,
                () -> batteryService.searchBatteriesByPostcodeRange(minPostcode, maxPostcode, minCapacity, maxCapacity));
        assertEquals("Failed to search batteries", thrown.getMessage());
        assertInstanceOf(EmptyResultDataAccessException.class, thrown.getCause());
        verify(batteryRepository, times(1)).findByPostcodeRangeAndCapacity(minPostcode, maxPostcode, minCapacity, maxCapacity);
    }
}