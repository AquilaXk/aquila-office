package com.aquilaoffice.adapter.out.token;

import com.aquilaoffice.application.port.out.GenerateWorkspaceInviteTokenPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * UUID 문자열로 워크스페이스 초대 token을 생성하는 adapter다.
 */
@Component
public class UuidWorkspaceInviteTokenGenerator implements GenerateWorkspaceInviteTokenPort {

    @Override
    public String generateToken() {
        return UUID.randomUUID().toString();
    }
}
