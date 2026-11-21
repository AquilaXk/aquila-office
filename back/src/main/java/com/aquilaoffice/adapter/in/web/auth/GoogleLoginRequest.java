package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.GoogleLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Google 로그인 HTTP 요청이다.
 */
@Schema(description = "Google OAuth id token 로그인 요청")
public record GoogleLoginRequest(
        @Schema(description = "Google OAuth id token", example = "eyJhbGciOiJSUzI1NiJ9...")
        @NotBlank String idToken) {

    public GoogleLoginCommand toCommand() {
        return new GoogleLoginCommand(idToken);
    }
}
