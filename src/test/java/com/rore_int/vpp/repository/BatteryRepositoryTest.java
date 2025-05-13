package com.rore_int.vpp.repository;

import com.rore_int.vpp.entity.Battery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BatteryRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private BatteryRepository batteryRepository;

    private List<Battery> batteries;

    @BeforeEach
    void setUp() {
        batteryRepository.deleteAll();
        Battery battery1 = new Battery();
        battery1.setName("Cannington");
        battery1.setPostcode("6107");
        battery1.setCapacity(13500);

        Battery battery2 = new Battery();
        battery2.setName("Midland");
        battery2.setPostcode("6057");
        battery2.setCapacity(50500);

        batteries = Arrays.asList(battery1, battery2);
        batteryRepository.saveAll(batteries);
    }

    @AfterAll
    static void tearDown() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Test
    void saveAll_shouldPersistBatteries() {
        List<Battery> savedBatteries = batteryRepository.saveAll(batteries);

        assertNotNull(savedBatteries);
        assertEquals(2, savedBatteries.size());
        assertNotNull(savedBatteries.get(0).getId());
        assertEquals("Cannington", savedBatteries.get(0).getName());
        assertEquals("Midland", savedBatteries.get(1).getName());
    }

    @Test
    void findAll_shouldReturnAllBatteries() {
        List<Battery> foundBatteries = batteryRepository.findAll();

        assertEquals(2, foundBatteries.size());
        assertEquals("Cannington", foundBatteries.get(0).getName());
        assertEquals("Midland", foundBatteries.get(1).getName());
    }

    @Test
    void findByPostcodeRangeAndCapacity_shouldReturnBatteriesInRangeSortedByName() {
        List<Battery> foundBatteries = batteryRepository.findByPostcodeRangeAndCapacity("6000", "6200", 10000, 60000);

        assertEquals(2, foundBatteries.size());
        assertEquals("Cannington", foundBatteries.get(0).getName());
        assertEquals("Midland", foundBatteries.get(1).getName());
        assertEquals(13500, foundBatteries.get(0).getCapacity());
        assertEquals(50500, foundBatteries.get(1).getCapacity());
    }

    @Test
    void findByPostcodeRangeAndCapacity_shouldReturnEmptyWhenNoBatteriesInRange() {
        List<Battery> foundBatteries = batteryRepository.findByPostcodeRangeAndCapacity("7000", "7200", null, null);

        assertTrue(foundBatteries.isEmpty());
    }

    @Test
    void findByPostcodeRangeAndCapacity_shouldFilterByMinCapacityOnly() {
        List<Battery> foundBatteries = batteryRepository.findByPostcodeRangeAndCapacity("6000", "6200", 50000, null);

        assertEquals(1, foundBatteries.size());
        assertEquals("Midland", foundBatteries.get(0).getName());
        assertEquals(50500, foundBatteries.get(0).getCapacity());
    }

    @Test
    void findByPostcodeRangeAndCapacity_shouldFilterByMaxCapacityOnly() {
        List<Battery> foundBatteries = batteryRepository.findByPostcodeRangeAndCapacity("6000", "6200", null, 20000);

        assertEquals(1, foundBatteries.size());
        assertEquals("Cannington", foundBatteries.get(0).getName());
        assertEquals(13500, foundBatteries.get(0).getCapacity());
    }
}