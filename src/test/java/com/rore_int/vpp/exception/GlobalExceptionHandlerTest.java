package com.rore_int.vpp.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rore_int.vpp.entity.Battery;
import com.rore_int.vpp.exception.DatabaseException;
import com.rore_int.vpp.exception.ValidationException;
import com.rore_int.vpp.service.BatteryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatteryService batteryService;

    @Test
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        // Trigger MethodArgumentNotValidException with invalid Battery (empty name, invalid postcode)
        Battery battery = new Battery();
        battery.setName("");
        battery.setPostcode("123");
        battery.setCapacity(0);
        List<Battery> batteries = List.of(battery);
        mockMvc.perform(post("/api/batteries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batteries)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("name: Name is mandatory")))
                .andExpect(jsonPath("$.message").value(containsString("postcode: Postcode must be a 4-digit number")))
                .andExpect(jsonPath("$.message").value(containsString("capacity: Capacity must be positive")));
    }

    @Test
    void shouldHandleValidationException() throws Exception {
        // Mock BatteryService to throw ValidationException
        when(batteryService.createBatteries(any())).thenThrow(new ValidationException("minPostcode is mandatory"));
        Battery battery = new Battery();
        battery.setName("TestBattery");
        battery.setPostcode("6000");
        battery.setCapacity(10000);
        List<Battery> batteries = List.of(battery);
        mockMvc.perform(post("/api/batteries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batteries)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("minPostcode is mandatory"));
    }

    @Test
    void shouldHandleDatabaseException() throws Exception {
        // Mock BatteryService to throw DatabaseException
        when(batteryService.createBatteries(any())).thenThrow(new DatabaseException("Failed to save batteries", new RuntimeException()));
        Battery battery = new Battery();
        battery.setName("TestBattery");
        battery.setPostcode("6000");
        battery.setCapacity(10000);
        List<Battery> batteries = List.of(battery);
        mockMvc.perform(post("/api/batteries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batteries)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("DATABASE_ERROR"))
                .andExpect(jsonPath("$.message").value("An error occurred while accessing the database"));
    }

    @Test
    void shouldHandleMissingServletRequestParameterException() throws Exception {
        // Trigger MissingServletRequestParameterException by omitting minPostcode
        mockMvc.perform(get("/api/batteries/search")
                        .param("maxPostcode", "6200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Missing required parameter: minPostcode"));
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        // Mock BatteryService to throw a generic Exception
        when(batteryService.createBatteries(any())).thenThrow(new RuntimeException("Unexpected error"));
        Battery battery = new Battery();
        battery.setName("TestBattery");
        battery.setPostcode("6000");
        battery.setCapacity(10000);
        List<Battery> batteries = List.of(battery);
        mockMvc.perform(post("/api/batteries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batteries)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}