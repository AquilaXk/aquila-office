package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.ArtifactFileResult;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationPlanFileUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.ArtifactFileData;
import com.aquilaoffice.application.port.out.ArtifactTreeData;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceArtifactsPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetOrchestrationPlanFileServiceTest {

    @Test
    void getOrchestrationPlanFileLoadsFileInsidePlanDirectoryForWorkspaceMember() {
        FakeLoadWorkspaceArtifactsPort artifactPort = new FakeLoadWorkspaceArtifactsPort(new ArtifactFileData(
                101L,
                "plans/701/reports/requirements.md",
                "requirements.md",
                "text/markdown",
                128L,
                "# 요구사항"));
        GetOrchestrationPlanFileUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                artifactPort);

        ArtifactFileResult result = service.getOrchestrationPlanFile(
                new GetOrchestrationPlanFileQuery(101L, 701L, "reports/requirements.md", " google-1 "));

        assertThat(artifactPort.workspaceId).isEqualTo(101L);
        assertThat(artifactPort.path).isEqualTo("plans/701/reports/requirements.md");
        assertThat(result.workspaceId()).isEqualTo(101L);
        assertThat(result.path()).isEqualTo("plans/701/reports/requirements.md");
        assertThat(result.content()).contains("요구사항");
    }

    @Test
    void getOrchestrationPlanFileRejectsUnsafeRelativePathBeforeLoadingFile() {
        FakeLoadWorkspaceArtifactsPort artifactPort = new FakeLoadWorkspaceArtifactsPort(new ArtifactFileData(
                101L,
                "plans/701/secret.txt",
                "secret.txt",
                "text/plain",
                6L,
                "secret"));
        GetOrchestrationPlanFileUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.of(workspaceDetail()),
                artifactPort);

        assertThatThrownBy(() -> service.getOrchestrationPlanFile(
                        new GetOrchestrationPlanFileQuery(101L, 701L, "../secret.txt", "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("artifact path is outside orchestration plan root");
        assertThat(artifactPort.called).isFalse();
    }

    @Test
    void getOrchestrationPlanFileRejectsRequesterWithoutWorkspaceMembership() {
        FakeLoadWorkspaceArtifactsPort artifactPort = new FakeLoadWorkspaceArtifactsPort(new ArtifactFileData(
                101L,
                "plans/701/reports/requirements.md",
                "requirements.md",
                "text/markdown",
                128L,
                "# 요구사항"));
        GetOrchestrationPlanFileUseCase service = newService(
                Optional.of(Member.createUser("google-1", "user@example.com", "User")),
                Optional.empty(),
                artifactPort);

        assertThatThrownBy(() -> service.getOrchestrationPlanFile(
                        new GetOrchestrationPlanFileQuery(101L, 701L, "reports/requirements.md", "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
        assertThat(artifactPort.called).isFalse();
    }

    private static GetOrchestrationPlanFileUseCase newService(
            Optional<Member> member,
            Optional<GetWorkspaceDetailResult> workspace,
            LoadWorkspaceArtifactsPort artifactPort) {
        return new GetOrchestrationPlanFileService(
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
            throw new UnsupportedOperationException("not used in orchestration plan file service test");
        }

        @Override
        public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                Long workspaceId,
                String googleSub) {
            return workspace;
        }
    }

    private static class FakeLoadWorkspaceArtifactsPort implements LoadWorkspaceArtifactsPort {
        private final ArtifactFileData fileData;
        private boolean called;
        private Long workspaceId;
        private String path;

        private FakeLoadWorkspaceArtifactsPort(ArtifactFileData fileData) {
            this.fileData = fileData;
        }

        @Override
        public ArtifactTreeData loadTree(Long workspaceId) {
            throw new UnsupportedOperationException("not used in orchestration plan file service test");
        }

        @Override
        public ArtifactFileData loadFile(Long workspaceId, String path) {
            this.called = true;
            this.workspaceId = workspaceId;
            this.path = path;
            return fileData;
        }
    }
}
