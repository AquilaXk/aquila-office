package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanArtifactUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.in.OrchestrationPlanArtifactResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationPlanArtifactPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.OrchestrationPlanArtifactData;
import com.aquilaoffice.application.port.out.OrchestrationPlanFileData;
import com.aquilaoffice.application.port.out.OrchestrationPlanStepData;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrchestrationPlanArtifactServiceTest {

    @Test
    void getOrchestrationPlanArtifactMapsPlanForWorkspaceMember() {
        FakeLoadOrchestrationPlanArtifactPort artifactPort = new FakeLoadOrchestrationPlanArtifactPort(
                new OrchestrationPlanArtifactData(
                        101L,
                        701L,
                        List.of(new OrchestrationPlanStepData(
                                11L,
                                "requirements",
                                1,
                                "요구사항 확인",
                                List.of(new OrchestrationPlanFileData(
                                        "plans/701/reports/requirements.md",
                                        "requirements.md",
                                        "text/markdown",
                                        128L,
                                        true))))));
        GetOrchestrationPlanArtifactUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                artifactPort);

        OrchestrationPlanArtifactResult result = service.getOrchestrationPlanArtifact(
                new GetOrchestrationPlanArtifactQuery(101L, 701L, " google-1 "));

        assertThat(artifactPort.workspaceId).isEqualTo(101L);
        assertThat(artifactPort.planId).isEqualTo(701L);
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.planId()).isEqualTo(701L);
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).stepKey()).isEqualTo("requirements");
        assertThat(result.steps().get(0).files()).hasSize(1);
        assertThat(result.steps().get(0).files().get(0).exists()).isTrue();
        assertThat(result.steps().get(0).files().get(0).sizeBytes()).isEqualTo(128L);
    }

    @Test
    void getOrchestrationPlanArtifactRejectsRequesterWithoutWorkspaceMembership() {
        FakeLoadOrchestrationPlanArtifactPort artifactPort = new FakeLoadOrchestrationPlanArtifactPort(
                new OrchestrationPlanArtifactData(101L, 701L, List.of()));
        GetOrchestrationPlanArtifactUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.empty(),
                artifactPort);

        assertThatThrownBy(() -> service.getOrchestrationPlanArtifact(
                        new GetOrchestrationPlanArtifactQuery(101L, 701L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
        assertThat(artifactPort.called).isFalse();
    }

    private static GetOrchestrationPlanArtifactUseCase newService(
            Optional<Member> member,
            Optional<GetWorkspaceDetailResult> workspace,
            LoadOrchestrationPlanArtifactPort artifactPort) {
        return new GetOrchestrationPlanArtifactService(
                new FakeLoadMemberPort(member),
                new FakeLoadWorkspacePort(workspace),
                artifactPort);
    }

    private static GetWorkspaceDetailResult workspaceDetail() {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static class FakeLoadMemberPort implements LoadMemberPort {
        private final Optional<Member> member;

        private FakeLoadMemberPort(Optional<Member> member) {
            this.member = member;
        }

        @Override
        public Optional<Member> findByGoogleSub(String googleSub) {
            return member;
        }
    }

    private static class FakeLoadWorkspacePort implements LoadWorkspacePort {
        private final Optional<GetWorkspaceDetailResult> workspace;

        private FakeLoadWorkspacePort(Optional<GetWorkspaceDetailResult> workspace) {
            this.workspace = workspace;
        }

        @Override
        public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
            throw new UnsupportedOperationException("not used in orchestration plan artifact service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return workspace;
        }
    }

    private static class FakeLoadOrchestrationPlanArtifactPort implements LoadOrchestrationPlanArtifactPort {
        private final OrchestrationPlanArtifactData artifact;
        private boolean called;
        private Long workspaceId;
        private Long planId;

        private FakeLoadOrchestrationPlanArtifactPort(OrchestrationPlanArtifactData artifact) {
            this.artifact = artifact;
        }

        @Override
        public OrchestrationPlanArtifactData loadOrchestrationPlan(Long workspaceId, Long planId) {
            this.called = true;
            this.workspaceId = workspaceId;
            this.planId = planId;
            return artifact;
        }
    }
}
