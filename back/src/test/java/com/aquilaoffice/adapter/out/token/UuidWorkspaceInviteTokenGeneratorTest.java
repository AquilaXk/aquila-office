package com.aquilaoffice.adapter.out.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * UUID 기반 워크스페이스 초대 token generator가 저장 가능한 문자열 token을 만드는지 검증한다.
 */
class UuidWorkspaceInviteTokenGeneratorTest {

    @Test
    void generateTokenReturnsUuidString() {
        UuidWorkspaceInviteTokenGenerator generator = new UuidWorkspaceInviteTokenGenerator();

        String token = generator.generateToken();

        assertThat(UUID.fromString(token).toString()).isEqualTo(token);
    }
}
