package com.aquilaoffice.global.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 초대 메일 설정은 비활성 기본값과 활성 SMTP 설정 검증을 분리해서 다룬다.
 */
class WorkspaceInviteEmailPropertiesTest {

    @Test
    void disabledPropertiesAllowBlankMailSettings() {
        WorkspaceInviteEmailProperties properties = new WorkspaceInviteEmailProperties(false, " ", " ");

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.from()).isEqualTo("noreply@aquila-office.local");
        assertThat(properties.acceptUrlBase()).isEqualTo("http://localhost:3000/invites/accept");
    }

    @Test
    void enabledPropertiesRequireMailSettings() {
        assertThatThrownBy(() -> new WorkspaceInviteEmailProperties(true, " ", "http://localhost:3000/invites"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from must not be blank");
        assertThatThrownBy(() -> new WorkspaceInviteEmailProperties(true, "noreply@aquila-office.local", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("acceptUrlBase must not be blank");
    }
}
