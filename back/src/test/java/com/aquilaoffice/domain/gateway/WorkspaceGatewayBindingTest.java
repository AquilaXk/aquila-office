package com.aquilaoffice.domain.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 워크스페이스 gateway binding 도메인이 URL/token 검증과 token masking을 책임지는지 검증한다.
 */
class WorkspaceGatewayBindingTest {

    @Test
    void createTrimsValuesAndMasksToken() {
        WorkspaceGatewayBinding binding = WorkspaceGatewayBinding.create(
                101L,
                " https://gateway.example.com ",
                " gate_1234567890abcdef ",
                " google-1 ");

        assertThat(binding.getWorkspaceId()).isEqualTo(101L);
        assertThat(binding.getGatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(binding.getToken()).isEqualTo("gate_1234567890abcdef");
        assertThat(binding.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(binding.maskedToken()).isEqualTo("gate****cdef");
    }

    @Test
    void restoreKeepsIdentifierAndMasksShortToken() {
        WorkspaceGatewayBinding binding = WorkspaceGatewayBinding.restore(
                501L,
                101L,
                "http://localhost:4317",
                "short",
                "google-1");

        assertThat(binding.getId()).isEqualTo(501L);
        assertThat(binding.maskedToken()).isEqualTo("****");
    }

    @Test
    void createRejectsInvalidRequiredValues() {
        assertThatThrownBy(() -> WorkspaceGatewayBinding.create(
                        0L, "https://gateway.example.com", "token", "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspaceId must be positive");
        assertThatThrownBy(() -> WorkspaceGatewayBinding.create(
                        101L, "ftp://gateway.example.com", "token", "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gatewayUrl must start with http:// or https://");
        assertThatThrownBy(() -> WorkspaceGatewayBinding.create(
                        101L, "https://gateway.example.com", " ", "google-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("token must not be blank");
        assertThatThrownBy(() -> WorkspaceGatewayBinding.create(
                        101L, "https://gateway.example.com", "token", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdByGoogleSub must not be blank");
    }
}
