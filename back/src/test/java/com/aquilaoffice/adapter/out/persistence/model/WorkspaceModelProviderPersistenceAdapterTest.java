package com.aquilaoffice.adapter.out.persistence.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import com.aquilaoffice.domain.workspace.Workspace;
import com.aquilaoffice.global.config.properties.ExternalTokenEncryptionProperties;
import com.aquilaoffice.global.security.AesGcmExternalTokenCipher;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({WorkspaceModelProviderPersistenceAdapter.class, AesGcmExternalTokenCipher.class})
@EnableConfigurationProperties(ExternalTokenEncryptionProperties.class)
class WorkspaceModelProviderPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private WorkspaceModelProviderJpaRepository modelProviderJpaRepository;

    @Autowired
    private WorkspaceModelProviderPersistenceAdapter adapter;

    @Test
    void persistsEncryptedApiKeyWithContextAadUpdatesAndSoftDeletes() {
        MemberJpaEntity ownerEntity = memberJpaRepository.save(
                MemberJpaEntity.create("owner-sub", "owner@example.com", "Owner", MemberRole.USER));
        WorkspaceJpaEntity workspace = workspaceJpaRepository.save(WorkspaceJpaEntity.from(
                Workspace.create("Model WS", "desc", Member.createUser("owner-sub", "owner@example.com", "Owner")),
                ownerEntity));

        WorkspaceModelProvider saved = adapter.save(WorkspaceModelProvider.create(
                workspace.getId(),
                ModelProviderKind.ANTHROPIC,
                ModelWireProtocol.ANTHROPIC_MESSAGES,
                "Anthropic Main",
                "https://api.anthropic.com",
                "sk-ant-api03-plainsecret1234",
                "claude-sonnet-4-20250514",
                List.of("claude-sonnet-4-20250514", "claude-opus-4-1-20250805"),
                1,
                true,
                "owner-sub"));

        WorkspaceModelProviderJpaEntity rawEntity = modelProviderJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(rawEntity.getApiKey()).startsWith("enc:v2:");
        assertThat(rawEntity.getApiKey()).doesNotContain("sk-ant-api03-plainsecret1234");
        assertThat(saved.getApiKey()).isEqualTo("sk-ant-api03-plainsecret1234");

        saved.recordConnectionCheck(
                ModelProviderConnectionStatus.CONNECTED,
                84L,
                LocalDateTime.now(ZoneOffset.UTC),
                null);
        WorkspaceModelProvider updated = adapter.save(saved);
        assertThat(updated.getLastStatus()).isEqualTo(ModelProviderConnectionStatus.CONNECTED);
        assertThat(updated.getLastLatencyMs()).isEqualTo(84L);

        updated.markDeleted(LocalDateTime.now(ZoneOffset.UTC));
        adapter.save(updated);
        assertThat(adapter.findActiveByWorkspaceId(workspace.getId())).isEmpty();
    }
}
