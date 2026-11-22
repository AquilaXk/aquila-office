package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateWorkspaceModelProviderCommand;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.TestWorkspaceModelProviderCommand;
import com.aquilaoffice.application.port.in.UpdateWorkspaceModelProviderCommand;
import com.aquilaoffice.application.port.in.WorkspaceModelConnectionTestResult;
import com.aquilaoffice.application.port.in.WorkspaceModelProviderResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadModelCatalogPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveWorkspaceModelProviderPort;
import com.aquilaoffice.application.port.out.TestWorkspaceModelProviderPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.model.ModelCatalogModel;
import com.aquilaoffice.domain.model.ModelCatalogPreset;
import com.aquilaoffice.domain.model.ModelProviderConnectionStatus;
import com.aquilaoffice.domain.model.ModelProviderKind;
import com.aquilaoffice.domain.model.ModelProviderTier;
import com.aquilaoffice.domain.model.ModelWireProtocol;
import com.aquilaoffice.domain.model.WorkspaceModelProvider;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import com.aquilaoffice.global.security.SecretRedactor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class WorkspaceModelProviderServiceTest {

    @Test
    void crudPreservesApiKeyOnBlankUpdateMasksSecretsAndRecordsRedactedConnectionChecks() {
        InMemoryProviderStore store = new InMemoryProviderStore();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-22T01:00:00Z"), ZoneOffset.UTC);
        LoadMemberPort loadMemberPort = googleSub -> "admin-1".equals(googleSub)
                ? Optional.of(Member.restore(1L, "admin-1", "admin@example.com", "Admin", MemberRole.USER))
                : Optional.of(Member.restore(2L, "member-1", "m@example.com", "Member", MemberRole.USER));
        LoadWorkspacePort loadWorkspacePort = new LoadWorkspacePort() {
            @Override
            public List<com.aquilaoffice.application.port.in.ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(
                    String googleSub) {
                return List.of();
            }

            @Override
            public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                    Long workspaceId,
                    String googleSub) {
                WorkspaceMemberRole role = "admin-1".equals(googleSub)
                        ? WorkspaceMemberRole.ADMIN
                        : WorkspaceMemberRole.MEMBER;
                return Optional.of(new GetWorkspaceDetailResult(
                        workspaceId, "WS", null, googleSub, role, LocalDateTime.now(ZoneOffset.UTC)));
            }
        };
        TestWorkspaceModelProviderPort testPort = req -> new WorkspaceModelConnectionTestResult(
                false,
                ModelProviderConnectionStatus.FAILED,
                42L,
                List.of(),
                req.modelId(),
                null,
                null,
                null,
                "upstream failed using Bearer " + req.apiKey());
        LoadModelCatalogPort catalogPort = () -> List.of(new ModelCatalogPreset(
                ModelProviderKind.OPENAI,
                "OpenAI",
                "https://api.openai.com/v1",
                ModelWireProtocol.OPENAI_RESPONSES,
                ModelProviderTier.PAID,
                true,
                List.of(new ModelCatalogModel("gpt-5.4", "GPT-5.4", 400000, 32768, true, true, true))));

        WorkspaceModelProviderService service = new WorkspaceModelProviderService(
                loadMemberPort,
                loadWorkspacePort,
                store,
                store,
                testPort,
                catalogPort,
                SecretRedactor::redactText,
                fixedClock);

        // 일반 멤버는 등록 불가
        assertThatThrownBy(() -> service.create(new CreateWorkspaceModelProviderCommand(
                10L, "member-1", ModelProviderKind.OPENAI, null, "OpenAI",
                "https://api.openai.com/v1", "sk-secret-key-9999", "gpt-5.4", null, 1, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("admin");

        WorkspaceModelProviderResult created = service.create(new CreateWorkspaceModelProviderCommand(
                10L, "admin-1", ModelProviderKind.OPENAI, null, "OpenAI",
                "https://api.openai.com/v1", "sk-secret-key-9999", "gpt-5.4",
                List.of("gpt-5.4", "gpt-5.4-mini"), 10, true));
        assertThat(created.hasApiKey()).isTrue();
        assertThat(created.maskedApiKey()).isEqualTo("sk-s...9999");

        // 일반 멤버 목록 조회 가능
        assertThat(service.list(10L, "member-1")).hasSize(1);

        // 수정 시 apiKey 미전달 시 기존 키 보존
        WorkspaceModelProviderResult updated = service.update(new UpdateWorkspaceModelProviderCommand(
                10L, created.providerId(), "admin-1", "OpenAI Primary", null, null, "  ", null, null, 5, true));
        assertThat(updated.displayName()).isEqualTo("OpenAI Primary");
        assertThat(updated.maskedApiKey()).isEqualTo("sk-s...9999");

        // 연결 테스트 결과 저장 시 민감정보 마스킹
        WorkspaceModelConnectionTestResult testResult = service.testConnection(new TestWorkspaceModelProviderCommand(
                10L, "admin-1", created.providerId(), null, null, null, null, null, "completion"));
        assertThat(testResult.ok()).isFalse();
        assertThat(testResult.message()).doesNotContain("sk-secret-key-9999");
        assertThat(service.list(10L, "admin-1").getFirst().lastStatus())
                .isEqualTo(ModelProviderConnectionStatus.FAILED);

        service.delete(10L, created.providerId(), "admin-1");
        assertThat(service.list(10L, "admin-1")).isEmpty();
    }

    private static final class InMemoryProviderStore
            implements LoadWorkspaceModelProviderPort, SaveWorkspaceModelProviderPort {

        private final AtomicLong seq = new AtomicLong(1L);
        private final Map<Long, WorkspaceModelProvider> rows = new LinkedHashMap<>();

        @Override
        public List<WorkspaceModelProvider> findActiveByWorkspaceId(Long workspaceId) {
            return rows.values().stream()
                    .filter(p -> p.getWorkspaceId().equals(workspaceId) && p.getDeletedAt() == null)
                    .toList();
        }

        @Override
        public Optional<WorkspaceModelProvider> findActiveByWorkspaceIdAndProviderId(
                Long workspaceId,
                Long providerId) {
            return Optional.ofNullable(rows.get(providerId))
                    .filter(p -> p.getWorkspaceId().equals(workspaceId) && p.getDeletedAt() == null);
        }

        @Override
        public WorkspaceModelProvider save(WorkspaceModelProvider provider) {
            long id = provider.getId() != null ? provider.getId() : seq.getAndIncrement();
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            WorkspaceModelProvider stored = WorkspaceModelProvider.restore(
                    id,
                    provider.getWorkspaceId(),
                    provider.getProviderKind(),
                    provider.getWireProtocol(),
                    provider.getDisplayName(),
                    provider.getBaseUrl(),
                    provider.getApiKey(),
                    provider.getDefaultModel(),
                    provider.getEnabledModels(),
                    provider.getPriorityOrder(),
                    provider.isEnabled(),
                    provider.getLastStatus(),
                    provider.getLastLatencyMs(),
                    provider.getLastCheckedAt(),
                    provider.getLastError(),
                    provider.getCreatedByGoogleSub(),
                    provider.getCreatedAt() != null ? provider.getCreatedAt() : now,
                    now,
                    provider.getDeletedAt());
            rows.put(id, stored);
            return stored;
        }
    }
}
