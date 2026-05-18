package com.aquilaoffice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.OrchestrationStepCommand;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.OrchestrationSessionStatusUpdate;
import com.aquilaoffice.application.port.out.OrchestrationStepTaskUpdate;
import com.aquilaoffice.application.port.out.SaveOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SavedOrchestrationSession;
import com.aquilaoffice.application.port.out.SavedOrchestrationStep;
import com.aquilaoffice.application.port.out.SavedTask;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.application.port.out.TaskDetail;
import com.aquilaoffice.application.port.out.UpdateOrchestrationSessionStatusPort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationStepPort;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFileSyncStatus;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrchestrationDispatchServiceTest {

    @Test
    void createSessionSavesValidatedPlan() {
        FakeSaveOrchestrationSessionPort saveSessionPort = new FakeSaveOrchestrationSessionPort();
        CreateOrchestrationSessionService service = new CreateOrchestrationSessionService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail())),
                new FakeLoadTaskPort(Optional.of(sourceTask())),
                new FakeLoadWorkspaceAgentPort(List.of(
                        savedAgent(501L, WorkspaceAgentStatus.READY),
                        savedAgent(502L, WorkspaceAgentStatus.READY))),
                saveSessionPort);

        CreateOrchestrationSessionResult result = service.createSession(new CreateOrchestrationSessionCommand(
                101L,
                301L,
                " google-1 ",
                List.of(
                        new OrchestrationStepCommand("plan", "계획 정리", 501L, List.of()),
                        new OrchestrationStepCommand("backend", "서비스 구현", 502L, List.of("plan")))));

        assertThat(saveSessionPort.saved.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(saveSessionPort.saved.getSteps()).extracting(OrchestrationStep::getStepKey)
                .containsExactly("plan", "backend");
        assertThat(result.sessionId()).isEqualTo(901L);
        assertThat(result.status()).isEqualTo(OrchestrationSessionStatus.PLANNED);
        assertThat(result.steps()).extracting("stepKey").containsExactly("plan", "backend");
    }

    @Test
    void createSessionRejectsRequesterWithoutWorkspaceMembership() {
        CreateOrchestrationSessionService service = new CreateOrchestrationSessionService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.empty()),
                new FakeLoadTaskPort(Optional.of(sourceTask())),
                new FakeLoadWorkspaceAgentPort(List.of(savedAgent(501L, WorkspaceAgentStatus.READY))),
                new FakeSaveOrchestrationSessionPort());

        assertThatThrownBy(() -> service.createSession(new CreateOrchestrationSessionCommand(
                        101L,
                        301L,
                        "google-1",
                        List.of(new OrchestrationStepCommand("plan", "계획 정리", 501L, List.of())))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace membership not found");
    }

    @Test
    void dispatchSessionRunsReadyStepsInDependencyOrderAndCompletesSession() {
        FakeOrchestrationSessionStore sessionStore = new FakeOrchestrationSessionStore(OrchestrationSession.restore(
                901L,
                101L,
                301L,
                "google-1",
                OrchestrationSessionStatus.PLANNED,
                List.of(
                        OrchestrationStep.restore(1001L, "plan", "계획 정리", 501L, null, List.of(),
                                OrchestrationStepStatus.PENDING),
                        OrchestrationStep.restore(1002L, "backend", "서비스 구현", 502L, null, List.of("plan"),
                                OrchestrationStepStatus.PENDING))));
        FakeSaveTaskPort saveTaskPort = new FakeSaveTaskPort();
        FakeRunTaskUseCase runTaskUseCase = new FakeRunTaskUseCase();
        DispatchOrchestrationSessionService service = new DispatchOrchestrationSessionService(
                new FakeLoadMemberPort(Optional.of(Member.createUser("google-1", "user@example.com", "User"))),
                new FakeLoadWorkspacePort(Optional.of(workspaceDetail())),
                sessionStore,
                saveTaskPort,
                sessionStore,
                sessionStore,
                runTaskUseCase);

        DispatchOrchestrationSessionResult result = service.dispatchSession(
                new DispatchOrchestrationSessionCommand(101L, 901L, "google-1", true));

        assertThat(saveTaskPort.savedTasks).extracting(Task::getAssignedAgentId).containsExactly(501L, 502L);
        assertThat(runTaskUseCase.commands).extracting(RunTaskCommand::taskId).containsExactly(401L, 402L);
        assertThat(runTaskUseCase.commands).extracting(RunTaskCommand::createPr).containsExactly(true, true);
        assertThat(result.dispatchedCount()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(OrchestrationSessionStatus.SUCCEEDED);
        assertThat(result.steps()).extracting("status")
                .containsExactly(OrchestrationStepStatus.SUCCEEDED, OrchestrationStepStatus.SUCCEEDED);
    }

    private static TaskDetail sourceTask() {
        return new TaskDetail(
                301L,
                101L,
                "google-1",
                "상위 작업",
                "여러 Agent가 나눠 수행할 작업",
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                null,
                null,
                TaskStatus.REQUESTED,
                LocalDateTime.of(2026, 5, 18, 12, 0),
                LocalDateTime.of(2026, 5, 18, 12, 0));
    }

    private static com.aquilaoffice.application.port.in.GetWorkspaceDetailResult workspaceDetail() {
        return new com.aquilaoffice.application.port.in.GetWorkspaceDetailResult(
                101L,
                "Team",
                "Description",
                "google-1",
                WorkspaceMemberRole.MEMBER,
                LocalDateTime.of(2026, 5, 18, 12, 0));
    }

    private static SavedWorkspaceAgent savedAgent(Long agentId, WorkspaceAgentStatus status) {
        return new SavedWorkspaceAgent(
                agentId,
                101L,
                "Agent " + agentId,
                WorkspaceAgentCategory.BACKEND,
                "gateway-agent-" + agentId,
                ".aquila/workspaces/101",
                status,
                null,
                "google-1",
                LocalDateTime.of(2026, 5, 18, 12, 0),
                LocalDateTime.of(2026, 5, 18, 12, 0),
                List.of(new SavedWorkspaceAgentSkillFile(
                        agentId + 1000L,
                        "AGENTS.md",
                        WorkspaceAgentSkillFileSyncStatus.SYNCED,
                        null)));
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
        private final Optional<com.aquilaoffice.application.port.in.GetWorkspaceDetailResult> detail;

        private FakeLoadWorkspacePort(
                Optional<com.aquilaoffice.application.port.in.GetWorkspaceDetailResult> detail) {
            this.detail = detail;
        }

        @Override
        public List<com.aquilaoffice.application.port.in.ListWorkspacesResult> findJoinedWorkspacesByMemberGoogleSub(
                String googleSub) {
            throw new UnsupportedOperationException("not used in orchestration test");
        }

        @Override
        public Optional<com.aquilaoffice.application.port.in.GetWorkspaceDetailResult>
                findJoinedWorkspaceByIdAndMemberGoogleSub(Long workspaceId, String googleSub) {
            return detail;
        }
    }

    private static class FakeLoadTaskPort implements LoadTaskPort {
        private final Optional<TaskDetail> task;

        private FakeLoadTaskPort(Optional<TaskDetail> task) {
            this.task = task;
        }

        @Override
        public Optional<TaskDetail> findByWorkspaceIdAndTaskId(Long workspaceId, Long taskId) {
            return task;
        }
    }

    private static class FakeLoadWorkspaceAgentPort implements LoadWorkspaceAgentPort {
        private final List<SavedWorkspaceAgent> agents;

        private FakeLoadWorkspaceAgentPort(List<SavedWorkspaceAgent> agents) {
            this.agents = agents;
        }

        @Override
        public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
            return agents;
        }

        @Override
        public Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(Long workspaceId, Long agentId) {
            return agents.stream()
                    .filter(agent -> agent.agentId().equals(agentId))
                    .findFirst();
        }
    }

    private static class FakeSaveOrchestrationSessionPort implements SaveOrchestrationSessionPort {
        private OrchestrationSession saved;

        @Override
        public SavedOrchestrationSession save(OrchestrationSession session) {
            this.saved = session;
            return savedSession(901L, session);
        }
    }

    private static class FakeOrchestrationSessionStore implements
            LoadOrchestrationSessionPort,
            UpdateOrchestrationStepPort,
            UpdateOrchestrationSessionStatusPort {
        private OrchestrationSession current;

        private FakeOrchestrationSessionStore(OrchestrationSession current) {
            this.current = current;
        }

        @Override
        public Optional<OrchestrationSession> findByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId) {
            return Optional.of(current);
        }

        @Override
        public SavedOrchestrationStep updateStep(OrchestrationStepTaskUpdate update) {
            List<OrchestrationStep> steps = current.getSteps().stream()
                    .map(step -> step.getStepKey().equals(update.stepKey())
                            ? OrchestrationStep.restore(
                                    step.getId(),
                                    step.getStepKey(),
                                    step.getInstruction(),
                                    step.getAgentId(),
                                    update.taskId(),
                                    step.getDependsOn(),
                                    update.status())
                            : step)
                    .toList();
            current = OrchestrationSession.restore(
                    current.getId(),
                    current.getWorkspaceId(),
                    current.getSourceTaskId(),
                    current.getCreatedByGoogleSub(),
                    current.getStatus(),
                    steps);
            return savedStep(current.getSteps().stream()
                    .filter(step -> step.getStepKey().equals(update.stepKey()))
                    .findFirst()
                    .orElseThrow());
        }

        @Override
        public SavedOrchestrationSession updateStatus(OrchestrationSessionStatusUpdate update) {
            current = OrchestrationSession.restore(
                    current.getId(),
                    current.getWorkspaceId(),
                    current.getSourceTaskId(),
                    current.getCreatedByGoogleSub(),
                    update.status(),
                    current.getSteps());
            return savedSession(current.getId(), current);
        }
    }

    private static class FakeSaveTaskPort implements SaveTaskPort {
        private long nextTaskId = 401L;
        private final List<Task> savedTasks = new ArrayList<>();

        @Override
        public SavedTask save(Task task) {
            savedTasks.add(task);
            return new SavedTask(
                    nextTaskId++,
                    task.getWorkspaceId(),
                    task.getCreatedByGoogleSub(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getType(),
                    task.getPriority(),
                    task.getAssignedAgentId(),
                    task.getSourceRef(),
                    task.getStatus(),
                    LocalDateTime.of(2026, 5, 18, 12, 0),
                    LocalDateTime.of(2026, 5, 18, 12, 0));
        }
    }

    private static class FakeRunTaskUseCase implements RunTaskUseCase {
        private final List<RunTaskCommand> commands = new ArrayList<>();

        @Override
        public RunTaskResult runTask(RunTaskCommand command) {
            commands.add(command);
            return new RunTaskResult(
                    command.workspaceId(),
                    command.taskId(),
                    801L + commands.size(),
                    500L + commands.size(),
                    TaskStatus.COMPLETED,
                    TaskExecutionStatus.SUCCEEDED,
                    "완료",
                    null);
        }
    }

    private static SavedOrchestrationSession savedSession(Long sessionId, OrchestrationSession session) {
        return new SavedOrchestrationSession(
                sessionId,
                session.getWorkspaceId(),
                session.getSourceTaskId(),
                session.getCreatedByGoogleSub(),
                session.getStatus(),
                session.getSteps().stream().map(OrchestrationDispatchServiceTest::savedStep).toList(),
                LocalDateTime.of(2026, 5, 18, 12, 0),
                LocalDateTime.of(2026, 5, 18, 12, 0));
    }

    private static SavedOrchestrationStep savedStep(OrchestrationStep step) {
        return new SavedOrchestrationStep(
                step.getId() == null ? 1000L : step.getId(),
                step.getStepKey(),
                step.getInstruction(),
                step.getAgentId(),
                step.getTaskId(),
                step.getDependsOn(),
                step.getStatus());
    }
}
