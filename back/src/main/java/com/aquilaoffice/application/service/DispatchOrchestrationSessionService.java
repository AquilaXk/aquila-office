package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.DispatchOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.RunTaskCommand;
import com.aquilaoffice.application.port.in.RunTaskResult;
import com.aquilaoffice.application.port.in.RunTaskUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.OrchestrationSessionStatusUpdate;
import com.aquilaoffice.application.port.out.OrchestrationStepTaskUpdate;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SavedTask;
import com.aquilaoffice.application.port.out.UpdateOrchestrationSessionStatusPort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationStepPort;
import com.aquilaoffice.domain.execution.TaskExecutionStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import java.util.List;
import java.util.Objects;

/**
 * dependsOn이 충족된 orchestration step을 Task로 생성하고 worker Agent 실행으로 넘긴다.
 */
public class DispatchOrchestrationSessionService implements DispatchOrchestrationSessionUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadOrchestrationSessionPort loadOrchestrationSessionPort;
    private final SaveTaskPort saveTaskPort;
    private final UpdateOrchestrationStepPort updateOrchestrationStepPort;
    private final UpdateOrchestrationSessionStatusPort updateOrchestrationSessionStatusPort;
    private final RunTaskUseCase runTaskUseCase;

    public DispatchOrchestrationSessionService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadOrchestrationSessionPort loadOrchestrationSessionPort,
            SaveTaskPort saveTaskPort,
            UpdateOrchestrationStepPort updateOrchestrationStepPort,
            UpdateOrchestrationSessionStatusPort updateOrchestrationSessionStatusPort,
            RunTaskUseCase runTaskUseCase) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadOrchestrationSessionPort = Objects.requireNonNull(
                loadOrchestrationSessionPort, "loadOrchestrationSessionPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "saveTaskPort must not be null");
        this.updateOrchestrationStepPort =
                Objects.requireNonNull(updateOrchestrationStepPort, "updateOrchestrationStepPort must not be null");
        this.updateOrchestrationSessionStatusPort = Objects.requireNonNull(
                updateOrchestrationSessionStatusPort, "updateOrchestrationSessionStatusPort must not be null");
        this.runTaskUseCase = Objects.requireNonNull(runTaskUseCase, "runTaskUseCase must not be null");
    }

    @Override
    public DispatchOrchestrationSessionResult dispatchSession(DispatchOrchestrationSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeGoogleSub(command.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(command.workspaceId(), requesterGoogleSub);

        OrchestrationSession session = loadSession(command.workspaceId(), command.sessionId());
        if (session.getStatus().isTerminal()) {
            return OrchestrationResultMapper.toDispatchResult(session, 0);
        }
        if (session.getStatus() == OrchestrationSessionStatus.PLANNED) {
            updateOrchestrationSessionStatusPort.updateStatus(new OrchestrationSessionStatusUpdate(
                    command.workspaceId(),
                    command.sessionId(),
                    OrchestrationSessionStatus.RUNNING));
        }

        int dispatchedCount = 0;
        while (true) {
            session = loadSession(command.workspaceId(), command.sessionId());
            List<OrchestrationStep> readySteps = session.readySteps();
            if (readySteps.isEmpty()) {
                break;
            }
            for (OrchestrationStep step : readySteps) {
                dispatchStep(command, requesterGoogleSub, step);
                dispatchedCount++;
            }
        }

        session = completeSessionIfTerminal(command.workspaceId(), command.sessionId());
        return OrchestrationResultMapper.toDispatchResult(session, dispatchedCount);
    }

    private void dispatchStep(
            DispatchOrchestrationSessionCommand command,
            String requesterGoogleSub,
            OrchestrationStep step) {
        SavedTask task = saveTaskPort.save(Task.create(
                command.workspaceId(),
                requesterGoogleSub,
                step.getStepKey(),
                step.getInstruction(),
                TaskType.OTHER,
                TaskPriority.MEDIUM,
                step.getAgentId(),
                "orchestration:" + command.sessionId() + ":" + step.getStepKey()));
        RunTaskResult runResult = runTaskUseCase.runTask(new RunTaskCommand(
                command.workspaceId(),
                task.taskId(),
                requesterGoogleSub,
                command.createPr()));
        updateOrchestrationStepPort.updateStep(new OrchestrationStepTaskUpdate(
                command.workspaceId(),
                command.sessionId(),
                step.getStepKey(),
                task.taskId(),
                toStepStatus(runResult)));
    }

    private OrchestrationSession completeSessionIfTerminal(Long workspaceId, Long sessionId) {
        OrchestrationSession session = loadSession(workspaceId, sessionId);
        if (session.getSteps().stream().anyMatch(step -> step.getStatus() == OrchestrationStepStatus.FAILED)) {
            return toDomain(updateOrchestrationSessionStatusPort.updateStatus(new OrchestrationSessionStatusUpdate(
                    workspaceId,
                    sessionId,
                    OrchestrationSessionStatus.FAILED)));
        }
        if (session.getSteps().stream().allMatch(step -> step.getStatus() == OrchestrationStepStatus.SUCCEEDED)) {
            return toDomain(updateOrchestrationSessionStatusPort.updateStatus(new OrchestrationSessionStatusUpdate(
                    workspaceId,
                    sessionId,
                    OrchestrationSessionStatus.SUCCEEDED)));
        }
        return session;
    }

    private OrchestrationStepStatus toStepStatus(RunTaskResult runResult) {
        if (runResult.executionStatus() == TaskExecutionStatus.SUCCEEDED
                && runResult.taskStatus() == TaskStatus.COMPLETED) {
            return OrchestrationStepStatus.SUCCEEDED;
        }
        return OrchestrationStepStatus.FAILED;
    }

    private OrchestrationSession toDomain(
            com.aquilaoffice.application.port.out.SavedOrchestrationSession savedSession) {
        return OrchestrationSession.restore(
                savedSession.sessionId(),
                savedSession.workspaceId(),
                savedSession.sourceTaskId(),
                savedSession.createdByGoogleSub(),
                savedSession.status(),
                savedSession.steps().stream()
                        .map(step -> OrchestrationStep.restore(
                                step.stepId(),
                                step.stepKey(),
                                step.instruction(),
                                step.agentId(),
                                step.taskId(),
                                step.dependsOn(),
                                step.status()))
                        .toList());
    }

    private OrchestrationSession loadSession(Long workspaceId, Long sessionId) {
        return loadOrchestrationSessionPort
                .findByWorkspaceIdAndSessionId(workspaceId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("orchestration session not found"));
    }

    private void requireExistingMember(String googleSub) {
        loadMemberPort
                .findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalArgumentException("requester member not found"));
    }

    private void requireJoinedWorkspace(Long workspaceId, String googleSub) {
        loadWorkspacePort
                .findJoinedWorkspaceByIdAndMemberGoogleSub(workspaceId, googleSub)
                .orElseThrow(() -> new IllegalArgumentException("workspace membership not found"));
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
