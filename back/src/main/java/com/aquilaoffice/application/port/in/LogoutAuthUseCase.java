package com.aquilaoffice.application.port.in;

/**
 * 저장된 refresh token을 무효화하는 inbound port다.
 */
public interface LogoutAuthUseCase {

    void logout(LogoutAuthCommand command);
}
