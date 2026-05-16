package com.aquilaoffice.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 보안 설정이 공개 엔드포인트, 인증 필요 API, CORS preflight 계약을 지키는지 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLoginEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsPreflightAllowsLocalFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}
