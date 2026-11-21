package com.aquilaoffice.adapter.out.persistence.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadWorkspaceGatewayBindingPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceGatewayBindingPort;
import com.aquilaoffice.domain.gateway.GatewayConnectionStatus;
import com.aquilaoffice.domain.gateway.WorkspaceGatewayBinding;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import com.aquilaoffice.global.security.AesGcmExternalTokenCipher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * workspace gateway binding persistence adapter가 active binding을 단건으로 유지하는지 검증한다.
 */
@DataJpaTest
@Import({WorkspaceGatewayBindingPersistenceAdapter.class, AesGcmExternalTokenCipher.class})
@EnableConfigurationProperties(ExternalTokenEncryptionProperties.class)
class WorkspaceGatewayBindingPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private WorkspaceGatewayBindingJpaRepository workspaceGatewayBindingJpaRepository;

    @Autowired
    private SaveWorkspaceGatewayBindingPort saveWorkspaceGatewayBindingPort;

    @Autowired
    private LoadWorkspaceGatewayBindingPort loadWorkspaceGatewayBindingPort;

    @Test
    void savePersistsBindingAndLoadsLatestActiveBinding() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        WorkspaceGatewayBinding savedBinding = saveWorkspaceGatewayBindingPort.save(WorkspaceGatewayBinding.create(
                workspaceEntity.getId(),
                " https://gateway.example.com ",
                " gate_1234567890abcdef ",
                " creator-google "));

        assertThat(savedBinding.getId()).isNotNull();
        assertThat(savedBinding.getGatewayUrl()).isEqualTo("https://gateway.example.com");
        assertThat(workspaceGatewayBindingJpaRepository.findById(savedBinding.getId()))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getToken()).startsWith("enc:v1:test-key:");
                    assertThat(entity.getToken()).doesNotContain("gate_1234567890abcdef");
                });
        assertThat(loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(workspaceEntity.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(savedBinding.getId());
                    assertThat(found.getWorkspaceId()).isEqualTo(workspaceEntity.getId());
                    assertThat(found.maskedToken()).isEqualTo("gate****cdef");
                });
    }

    @Test
    void savePersistsAuthenticatedDeviceIdentityAndLastConnectionStatus() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        LocalDateTime authenticatedAt = LocalDateTime.of(2026, 5, 18, 10, 30);
        LocalDateTime lastCheckedAt = LocalDateTime.of(2026, 5, 18, 10, 31);

        WorkspaceGatewayBinding savedBinding = saveWorkspaceGatewayBindingPort.save(WorkspaceGatewayBinding.createAuthenticated(
                workspaceEntity.getId(),
                "https://gateway.example.com",
                "gate_1234567890abcdef",
                "creator-google",
                "gw-device-1",
                "Seoul Office Gateway",
                authenticatedAt,
                GatewayConnectionStatus.CONNECTED,
                lastCheckedAt,
                null));

        assertThat(workspaceGatewayBindingJpaRepository.findById(savedBinding.getId()))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getDeviceId()).isEqualTo("gw-device-1");
                    assertThat(entity.getDeviceName()).isEqualTo("Seoul Office Gateway");
                    assertThat(entity.getAuthenticatedAt()).isEqualTo(authenticatedAt);
                    assertThat(entity.getLastStatus()).isEqualTo(GatewayConnectionStatus.CONNECTED);
                    assertThat(entity.getLastCheckedAt()).isEqualTo(lastCheckedAt);
                    assertThat(entity.getLastError()).isNull();
                });
        assertThat(loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(workspaceEntity.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getDeviceId()).isEqualTo("gw-device-1");
                    assertThat(found.getDeviceName()).isEqualTo("Seoul Office Gateway");
                    assertThat(found.getAuthenticatedAt()).isEqualTo(authenticatedAt);
                    assertThat(found.getLastStatus()).isEqualTo(GatewayConnectionStatus.CONNECTED);
                    assertThat(found.getLastCheckedAt()).isEqualTo(lastCheckedAt);
                    assertThat(found.getLastError()).isNull();
                });
    }

    @Test
    void saveReplacesPreviousActiveBindingForWorkspace() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        WorkspaceGatewayBinding firstBinding = saveWorkspaceGatewayBindingPort.save(WorkspaceGatewayBinding.create(
                workspaceEntity.getId(),
                "https://first.example.com",
                "first-token-1234",
                "creator-google"));

        WorkspaceGatewayBinding secondBinding = saveWorkspaceGatewayBindingPort.save(WorkspaceGatewayBinding.create(
                workspaceEntity.getId(),
                "https://second.example.com",
                "second-token-5678",
                "creator-google"));

        assertThat(loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(workspaceEntity.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(secondBinding.getId());
                    assertThat(found.getGatewayUrl()).isEqualTo("https://second.example.com");
                });
        List<WorkspaceGatewayBindingJpaEntity> allEntities = workspaceGatewayBindingJpaRepository.findAll();
        assertThat(allEntities).hasSize(2);
        assertThat(allEntities)
                .filteredOn(entity -> entity.getId().equals(firstBinding.getId()))
                .singleElement()
                .satisfies(entity -> assertThat(entity.getDeletedAt()).isNotNull());
        assertThat(allEntities)
                .filteredOn(entity -> entity.getId().equals(secondBinding.getId()))
                .singleElement()
                .satisfies(entity -> assertThat(entity.getDeletedAt()).isNull());
    }

    @Test
    void findActiveByWorkspaceIdReturnsEmptyWhenNoBindingExists() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        assertThat(loadWorkspaceGatewayBindingPort.findActiveByWorkspaceId(workspaceEntity.getId())).isEmpty();
    }

    @Test
    void saveRejectsMissingWorkspaceEntity() {
        assertThatThrownBy(() -> saveWorkspaceGatewayBindingPort.save(WorkspaceGatewayBinding.create(
                        404L, "https://gateway.example.com", "token", "creator-google")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace entity not found");
        assertThat(workspaceGatewayBindingJpaRepository.count()).isZero();
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
