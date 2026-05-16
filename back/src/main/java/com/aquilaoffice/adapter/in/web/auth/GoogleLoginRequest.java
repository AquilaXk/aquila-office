package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.GoogleLoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Google 로그인 HTTP 요청이다.
 */
public record GoogleLoginRequest(@NotBlank String idToken) {

    public GoogleLoginCommand toCommand() {
        return new GoogleLoginCommand(idToken);
    }
}
