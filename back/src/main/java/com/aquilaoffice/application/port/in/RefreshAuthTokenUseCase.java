package com.aquilaoffice.application.port.in;

/**
 * 저장된 refresh token을 회전하고 새 token pair를 발급하는 inbound port다.
 */
public interface RefreshAuthTokenUseCase {

    RefreshAuthTokenResult refresh(RefreshAuthTokenCommand command);
}
