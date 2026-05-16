package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.LogoutAuthCommand;
import com.aquilaoffice.application.port.in.LogoutAuthUseCase;
import com.aquilaoffice.application.port.out.DeleteRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolveRefreshTokenPort;
import com.aquilaoffice.application.port.out.ResolvedRefreshToken;
import java.util.Objects;

/**
 * Refresh token payload 검증과 저장 token 삭제를 조율하는 application service다.
 */
public class LogoutAuthService implements LogoutAuthUseCase {

    private final ResolveRefreshTokenPort resolveRefreshTokenPort;
    private final DeleteRefreshTokenPort deleteRefreshTokenPort;

    public LogoutAuthService(
            ResolveRefreshTokenPort resolveRefreshTokenPort,
            DeleteRefreshTokenPort deleteRefreshTokenPort) {
        this.resolveRefreshTokenPort =
                Objects.requireNonNull(resolveRefreshTokenPort, "resolveRefreshTokenPort must not be null");
        this.deleteRefreshTokenPort =
                Objects.requireNonNull(deleteRefreshTokenPort, "deleteRefreshTokenPort must not be null");
    }

    @Override
    public void logout(LogoutAuthCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ResolvedRefreshToken resolvedRefreshToken = resolveRefreshTokenPort.resolve(command.refreshToken());
        boolean deleted = deleteRefreshTokenPort.deleteIfMatch(
                resolvedRefreshToken.memberId(),
                command.refreshToken());
        if (!deleted) {
            throw new IllegalArgumentException("refreshToken is invalid");
        }
    }
}
