package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.LogoutAuthCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 HTTP 요청이다.
 */
public record LogoutAuthRequest(@NotBlank String refreshToken) {

    public LogoutAuthCommand toCommand() {
        return new LogoutAuthCommand(refreshToken);
    }
}
