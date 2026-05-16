package com.aquilaoffice.adapter.in.web.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aquilaoffice.application.port.in.GoogleLoginCommand;
import com.aquilaoffice.application.port.in.GoogleLoginResult;
import com.aquilaoffice.application.port.in.GoogleLoginUseCase;
import com.aquilaoffice.global.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Auth web adapter가 Google 로그인 요청을 command로 변환하고 공통 응답 계약을 지키는지 검증한다.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleLoginUseCase googleLoginUseCase;

    @Test
    void loginWithGoogleReturnsTokenEnvelope() throws Exception {
        given(googleLoginUseCase.login(any(GoogleLoginCommand.class)))
                .willReturn(new GoogleLoginResult(
                        1L,
                        "User One",
                        "user@example.com",
                        "USER",
                        "access-token",
                        "refresh-token"));

        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\" id-token \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.name").value("User One"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        ArgumentCaptor<GoogleLoginCommand> commandCaptor = ArgumentCaptor.forClass(GoogleLoginCommand.class);
        verify(googleLoginUseCase).login(commandCaptor.capture());
        assertThat(commandCaptor.getValue().idToken()).isEqualTo("id-token");
    }

    @Test
    void loginWithGoogleRejectsBlankIdToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("400"));

        verifyNoInteractions(googleLoginUseCase);
    }

    @Test
    void loginWithGoogleReturnsUnauthorizedWhenTokenIsInvalid() throws Exception {
        willThrow(new IllegalArgumentException("invalid google id token"))
                .given(googleLoginUseCase)
                .login(any(GoogleLoginCommand.class));

        mockMvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }
}
