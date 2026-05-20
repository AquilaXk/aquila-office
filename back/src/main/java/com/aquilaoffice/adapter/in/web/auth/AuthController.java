package com.aquilaoffice.adapter.in.web.auth;

import com.aquilaoffice.application.port.in.GoogleLoginResult;
import com.aquilaoffice.application.port.in.GoogleLoginUseCase;
import com.aquilaoffice.application.port.in.LogoutAuthCommand;
import com.aquilaoffice.application.port.in.LogoutAuthUseCase;
import com.aquilaoffice.application.port.in.RefreshAuthTokenCommand;
import com.aquilaoffice.application.port.in.RefreshAuthTokenResult;
import com.aquilaoffice.application.port.in.RefreshAuthTokenUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 요청을 application use case로 전달하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Google OAuth 로그인과 토큰 세션 API")
public class AuthController {

    private final GoogleLoginUseCase googleLoginUseCase;
    private final RefreshAuthTokenUseCase refreshAuthTokenUseCase;
    private final LogoutAuthUseCase logoutAuthUseCase;
    private final AuthRefreshCookieFactory refreshCookieFactory;

    public AuthController(
            GoogleLoginUseCase googleLoginUseCase,
            RefreshAuthTokenUseCase refreshAuthTokenUseCase,
            LogoutAuthUseCase logoutAuthUseCase,
            AuthRefreshCookieFactory refreshCookieFactory) {
        this.googleLoginUseCase = googleLoginUseCase;
        this.refreshAuthTokenUseCase = refreshAuthTokenUseCase;
        this.logoutAuthUseCase = logoutAuthUseCase;
        this.refreshCookieFactory = refreshCookieFactory;
    }

    @PostMapping("/google/login")
    @Operation(summary = "Google id token으로 로그인하고 서비스 토큰을 발급한다")
    public ResponseEntity<RsData<GoogleLoginResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request) {
        try {
            GoogleLoginResult result = googleLoginUseCase.login(request.toCommand());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.issue(result.refreshToken()).toString())
                    .body(RsData.ok(GoogleLoginResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<RsData<RefreshAuthTokenResponse>> refreshToken(
            @CookieValue(name = AuthRefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        String currentRefreshToken = requireRefreshCookie(refreshToken);
        try {
            RefreshAuthTokenResult result = refreshAuthTokenUseCase.refresh(new RefreshAuthTokenCommand(currentRefreshToken));
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.issue(result.refreshToken()).toString())
                    .body(RsData.ok(RefreshAuthTokenResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<RsData<Void>> logout(
            @CookieValue(name = AuthRefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        String currentRefreshToken = requireRefreshCookie(refreshToken);
        try {
            logoutAuthUseCase.logout(new LogoutAuthCommand(currentRefreshToken));
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
                    .body(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private String requireRefreshCookie(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return refreshToken.trim();
    }
}
