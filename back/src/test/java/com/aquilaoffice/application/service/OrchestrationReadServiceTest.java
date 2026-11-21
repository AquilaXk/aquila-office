package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.GetOrchestrationSessionQuery;
import com.aquilaoffice.application.port.in.GetOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.GetWorkspaceDetailResult;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsQuery;
import com.aquilaoffice.application.port.in.ListOrchestrationSessionsResult;
import com.aquilaoffice.application.port.in.ListWorkspacesResult;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SavedOrchestrationSession;
import com.aquilaoffice.application.port.out.SavedOrchestrationStep;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Multi-Agent orchestration 목록·상세 조회 application service 계약을 검증한다.
 */
class OrchestrationReadServiceTest {

    @Test
    void listAndGetOrchestrationSessionsRequireMembershipAndMapSteps() {
        SavedOrchestrationSession saved = new SavedOrchestrationSession(
                901L,
                101L,
                301L,
                "google-1",
                OrchestrationSessionStatus.RUNNING,
                List.of(new SavedOrchestrationStep(
                        1001L,
                        "plan",
                        "설계",
                        501L,
                        401L,
                        List.of(),
                        OrchestrationStepStatus.SUCCEEDED)),
                LocalDateTime.of(2026, 5, 18, 10, 0),
                LocalDateTime.of(2026, 5, 18, 10, 1));

        LoadOrchestrationSessionPort sessionPort = new LoadOrchestrationSessionPort() {
            @Override
            public Optional<OrchestrationSession> findByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId) {
                return Optional.empty();
            }

            @Override
            public List<SavedOrchestrationSession> findByWorkspaceIdOrderByIdDesc(Long workspaceId) {
                return List.of(saved);
            }

            @Override
            public Optional<SavedOrchestrationSession> findDetailByWorkspaceIdAndSessionId(
                    Long workspaceId, Long sessionId) {
                return sessionId.equals(901L) ? Optional.of(saved) : Optional.empty();
            }
        };

        ListOrchestrationSessionsService listService = new ListOrchestrationSessionsService(
                memberPort(), workspacePort(Optional.of(workspaceDetail())), sessionPort);
        GetOrchestrationSessionService getService = new GetOrchestrationSessionService(
                memberPort(), workspacePort(Optional.of(workspaceDetail())), sessionPort);

        List<ListOrchestrationSessionsResult> listed =
                listService.listSessions(new ListOrchestrationSessionsQuery(101L, " google-1 "));
        assertThat(listed).singleElement().satisfies(item -> {
            assertThat(item.sessionId()).isEqualTo(901L);
            assertThat(item.status()).isEqualTo(OrchestrationSessionStatus.RUNNING);
            assertThat(item.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 18, 10, 0));
            assertThat(item.steps()).singleElement().satisfies(step -> {
                assertThat(step.stepKey()).isEqualTo("plan");
                assertThat(step.taskId()).isEqualTo(401L);
            });
        });

        GetOrchestrationSessionResult detail =
                getService.getSession(new GetOrchestrationSessionQuery(101L, 901L, "google-1"));
        assertThat(detail.sessionId()).isEqualTo(901L);
        assertThat(detail.steps()).hasSize(1);

        assertThatThrownBy(() -> getService.getSession(new GetOrchestrationSessionQuery(101L, 999L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orchestration session not found");
    }

    @Test
    void readServicesRejectNonMember() {
        LoadOrchestrationSessionPort sessionPort = new LoadOrchestrationSessionPort() {
            @Override
            public Optional<OrchestrationSession> findByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId) {
                return Optional.empty();
            }
        };
        ListOrchestrationSessionsService listService =
                new ListOrchestrationSessionsService(memberPort(), workspacePort(Optional.empty()), sessionPort);
        GetOrchestrationSessionService getService =
                new GetOrchestrationSessionService(memberPort(), workspacePort(Optional.empty()), sessionPort);

        assertThatThrownBy(() -> listService.listSessions(new ListOrchestrationSessionsQuery(101L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
        assertThatThrownBy(() -> getService.getSession(new GetOrchestrationSessionQuery(101L, 901L, "google-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
    }

    private static LoadMemberPort memberPort() {
        return googleSub -> Optional.of(Member.createUser(googleSub, googleSub + "@example.com", "User"));
    }

    private static LoadWorkspacePort workspacePort(Optional<GetWorkspaceDetailResult> detail) {
        return new LoadWorkspacePort() {
            @Override
            public List<ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(String googleSub) {
                return List.of();
            }

            @Override
            public Optional<GetWorkspaceDetailResult> findJoinedWorkspaceByIdAndMemberGoogleSub(
                    Long workspaceId, String googleSub) {
                return detail;
            }
        };
    }

    private static GetWorkspaceDetailResult workspaceDetail() {
        return new GetWorkspaceDetailResult(
                101L,
                "Team",
                "Desc",
                "google-1",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 18, 10, 0),
                null);
    }
}
