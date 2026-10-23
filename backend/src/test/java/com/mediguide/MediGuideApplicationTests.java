package com.mediguide;

import com.mediguide.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MediGuideApplicationTests {

    @Autowired
    private HealthService healthService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Verifies Spring application context loads successfully
    }

    @Test
    void healthServiceReturnsValidConnectedStatus() {
        Map<String, Object> status = healthService.getHealthStatus();
        assertNotNull(status);
        assertEquals("UP", status.get("status"));
        assertEquals("Connected", status.get("database"));
    }

    @Test
    void healthEndpointReturnsOkAndAccuratePayload() throws Exception {
        mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.database", is("Connected")));

        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.database", is("Connected")));
    }
}


