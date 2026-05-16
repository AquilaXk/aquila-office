package com.aquilaoffice.domain.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * GitHub 자격 증명 도메인의 입력 정규화, 수정, token 마스킹 규칙을 검증한다.
 */
class GithubCredentialTest {

    @Test
    void createTrimsFieldsAndMasksToken() {
        GithubCredential credential =
                GithubCredential.create(1L, " Main Token ", "ghp_1234567890abcdef", " creator-sub ");

        assertThat(credential.getWorkspaceId()).isEqualTo(1L);
        assertThat(credential.getDisplayName()).isEqualTo("Main Token");
        assertThat(credential.getToken()).isEqualTo("ghp_1234567890abcdef");
        assertThat(credential.getCreatedByGoogleSub()).isEqualTo("creator-sub");
        assertThat(credential.maskedToken()).isEqualTo("ghp_****cdef");
    }

    @Test
    void maskShortTokenWithoutLeakingCharacters() {
        GithubCredential credential = GithubCredential.create(1L, "Short", "short", "creator-sub");

        assertThat(credential.maskedToken()).isEqualTo("****");
    }

    @Test
    void updateKeepsExistingValuesForBlankInputs() {
        GithubCredential credential =
                GithubCredential.create(1L, "Main Token", "ghp_1234567890abcdef", "creator-sub");

        credential.update(" ", null);

        assertThat(credential.getDisplayName()).isEqualTo("Main Token");
        assertThat(credential.getToken()).isEqualTo("ghp_1234567890abcdef");

        credential.update(" Backup Token ", " ghp_fedcba0987654321 ");

        assertThat(credential.getDisplayName()).isEqualTo("Backup Token");
        assertThat(credential.getToken()).isEqualTo("ghp_fedcba0987654321");
        assertThat(credential.maskedToken()).isEqualTo("ghp_****4321");
    }

    @Test
    void createRejectsInvalidArguments() {
        assertThatThrownBy(() -> GithubCredential.create(null, "Main", "token", "creator-sub"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must not be null");
        assertThatThrownBy(() -> GithubCredential.create(1L, " ", "token", "creator-sub"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayName must not be blank");
        assertThatThrownBy(() -> GithubCredential.create(1L, "Main", " ", "creator-sub"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
        assertThatThrownBy(() -> GithubCredential.create(1L, "Main", "token", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdByGoogleSub must not be blank");
    }
}
