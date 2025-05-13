package com.rore_int.vpp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rore_int.vpp.entity.Battery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
public class BatteryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testConcurrentBatteryRegistrations() throws Exception {
        int numberOfThreads = 20;
        int batteriesPerRequest = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        List<Battery> batteries = new ArrayList<>();
        for (int i = 0; i < batteriesPerRequest; i++) {
            Battery battery = new Battery();
            battery.setName("Battery-" + i);
            battery.setPostcode("6000");
            battery.setCapacity(10000 + i);
            batteries.add(battery);
        }

        String requestBody = objectMapper.writeValueAsString(batteries);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(post("/api/batteries")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }

    @Test
    void testConcurrentBatterySearches() throws Exception {
        int numberOfThreads = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(get("/api/batteries/search")
                                    .param("minPostcode", "6000")
                                    .param("maxPostcode", "6200")
                                    .param("minCapacity", "10000")
                                    .param("maxCapacity", "60000"))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
    }

    @Test
    void testInvalidBatteryRegistration() throws Exception {
        Battery invalidBattery = new Battery();
        invalidBattery.setName(""); // Invalid: blank name
        invalidBattery.setPostcode("123"); // Invalid: not 4 digits
        invalidBattery.setCapacity(0); // Invalid: not positive

        String requestBody = objectMapper.writeValueAsString(Collections.singletonList(invalidBattery));

        mockMvc.perform(post("/api/batteries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("name: Name is mandatory")))
                .andExpect(jsonPath("$.message").value(containsString("postcode: Postcode must be a 4-digit number")))
                .andExpect(jsonPath("$.message").value(containsString("capacity: Capacity must be positive")));
    }

    @Test
    void testInvalidSearchParameters() throws Exception {
        mockMvc.perform(get("/api/batteries/search")
                        .param("minPostcode", "123")
                        .param("maxPostcode", "6200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("minPostcode must be a 4-digit number"));
    }
}