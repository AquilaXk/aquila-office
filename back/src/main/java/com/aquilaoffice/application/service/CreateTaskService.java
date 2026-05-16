package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateTaskCommand;
import com.aquilaoffice.application.port.in.CreateTaskResult;
import com.aquilaoffice.application.port.in.CreateTaskUseCase;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.task.Task;
import java.util.Objects;

/**
 * 워크스페이스 멤버가 작업을 생성할 때 담당 Agent가 같은 워크스페이스에서 실행 가능한지 검증한다.
 */
public class CreateTaskService implements CreateTaskUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;
    private final SaveTaskPort saveTaskPort;

    public CreateTaskService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            SaveTaskPort saveTaskPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadWorkspaceAgentPort =
                Objects.requireNonNull(loadWorkspaceAgentPort, "loadWorkspaceAgentPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "saveTaskPort must not be null");
    }

    @Override
    public CreateTaskResult createTask(CreateTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeGoogleSub(command.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(command.workspaceId(), requesterGoogleSub);
        requireReadyWorkspaceAgent(command.workspaceId(), command.assignedAgentId());

        Task task = Task.create(
                command.workspaceId(),
                requesterGoogleSub,
                command.title(),
                command.description(),
                command.type(),
                command.priority(),
                command.assignedAgentId());

        return TaskResultMapper.toCreateResult(saveTaskPort.save(task));
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

    private void requireReadyWorkspaceAgent(Long workspaceId, Long assignedAgentId) {
        if (assignedAgentId == null) {
            return;
        }
        var agent = loadWorkspaceAgentPort
                .findActiveByWorkspaceIdAndAgentId(workspaceId, assignedAgentId)
                .filter(savedAgent -> savedAgent.status() == WorkspaceAgentStatus.READY)
                .orElseThrow(() -> new IllegalArgumentException("ready workspace agent not found"));
        if (!workspaceId.equals(agent.workspaceId())) {
            throw new IllegalArgumentException("ready workspace agent not found");
        }
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
