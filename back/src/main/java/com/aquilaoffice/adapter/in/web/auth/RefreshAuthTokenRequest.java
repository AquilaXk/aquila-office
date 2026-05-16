package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.RefreshAuthTokenCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token 재발급 HTTP 요청이다.
 */
public record RefreshAuthTokenRequest(@NotBlank String refreshToken) {

    public RefreshAuthTokenCommand toCommand() {
        return new RefreshAuthTokenCommand(refreshToken);
    }
}
