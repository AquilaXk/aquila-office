package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.RefreshAuthTokenCommand;
import com.aquilaoffice.application.port.in.RefreshAuthTokenResult;
import com.aquilaoffice.application.port.in.RefreshAuthTokenUseCase;
import com.aquilaoffice.application.port.out.IssueAuthTokenPort;
import com.aquilaoffice.application.port.out.IssuedAuthTokens;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import com.aquilaoffice.application.port.out.RotateRefreshTokenPort;
import java.time.Clock;
import java.util.Objects;

/**
 * Refresh token payload 검증, token pair 발급, 저장 token 회전을 조율하는 application service다.
 */
public class RefreshAuthTokenService implements RefreshAuthTokenUseCase {

    private final ResolveRefreshTokenPort resolveRefreshTokenPort;
    private final IssueAuthTokenPort issueAuthTokenPort;
    private final RotateRefreshTokenPort rotateRefreshTokenPort;
    private final Clock clock;

    public RefreshAuthTokenService(
            ResolveRefreshTokenPort resolveRefreshTokenPort,
            IssueAuthTokenPort issueAuthTokenPort,
            RotateRefreshTokenPort rotateRefreshTokenPort,
            Clock clock) {
        this.resolveRefreshTokenPort =
                Objects.requireNonNull(resolveRefreshTokenPort, "resolveRefreshTokenPort must not be null");
        this.issueAuthTokenPort = Objects.requireNonNull(issueAuthTokenPort, "issueAuthTokenPort must not be null");
        this.rotateRefreshTokenPort =
                Objects.requireNonNull(rotateRefreshTokenPort, "rotateRefreshTokenPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public RefreshAuthTokenResult refresh(RefreshAuthTokenCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ResolvedRefreshToken resolvedRefreshToken = resolveRefreshTokenPort.resolve(command.refreshToken());
        IssuedAuthTokens issuedAuthTokens = issueAuthTokenPort.issue(
                resolvedRefreshToken.memberId(),
                resolvedRefreshToken.email(),
                resolvedRefreshToken.role(),
                resolvedRefreshToken.refreshSessionId());

        boolean rotated = rotateRefreshTokenPort.rotateIfMatch(
                resolvedRefreshToken.memberId(),
                resolvedRefreshToken.refreshSessionId(),
                command.refreshToken(),
                issuedAuthTokens.refreshToken(),
                issuedAuthTokens.refreshTokenExpiresAt(),
                clock.instant());
        if (!rotated) {
            throw new IllegalArgumentException("refreshToken is invalid");
        }

        return new RefreshAuthTokenResult(issuedAuthTokens.accessToken(), issuedAuthTokens.refreshToken());
    }
}
