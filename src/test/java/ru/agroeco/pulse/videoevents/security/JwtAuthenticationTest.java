package ru.agroeco.pulse.videoevents.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void issuesTokenAndRejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(post("/api/auth/token").contentType("application/json")
                        .content("{\"username\":\"operator\",\"password\":\"operator\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        mockMvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
    }
}
