package com.aquilaoffice.application.service;

import com.aquilaoffice.application.port.in.CreateOrchestrationSessionCommand;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionResult;
import com.aquilaoffice.application.port.in.CreateOrchestrationSessionUseCase;
import com.aquilaoffice.application.port.in.OrchestrationStepCommand;
import com.aquilaoffice.application.port.out.LoadMemberPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspacePort;
import com.aquilaoffice.application.port.out.SaveOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Multi Agent 실행 계획을 session과 step graph로 저장하기 전에 멤버, 작업, worker Agent를 검증한다.
 */
public class CreateOrchestrationSessionService implements CreateOrchestrationSessionUseCase {

    private final LoadMemberPort loadMemberPort;
    private final LoadWorkspacePort loadWorkspacePort;
    private final LoadTaskPort loadTaskPort;
    private final LoadWorkspaceAgentPort loadWorkspaceAgentPort;
    private final SaveOrchestrationSessionPort saveOrchestrationSessionPort;

    public CreateOrchestrationSessionService(
            LoadMemberPort loadMemberPort,
            LoadWorkspacePort loadWorkspacePort,
            LoadTaskPort loadTaskPort,
            LoadWorkspaceAgentPort loadWorkspaceAgentPort,
            SaveOrchestrationSessionPort saveOrchestrationSessionPort) {
        this.loadMemberPort = Objects.requireNonNull(loadMemberPort, "loadMemberPort must not be null");
        this.loadWorkspacePort = Objects.requireNonNull(loadWorkspacePort, "loadWorkspacePort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "loadTaskPort must not be null");
        this.loadWorkspaceAgentPort =
                Objects.requireNonNull(loadWorkspaceAgentPort, "loadWorkspaceAgentPort must not be null");
        this.saveOrchestrationSessionPort =
                Objects.requireNonNull(saveOrchestrationSessionPort, "saveOrchestrationSessionPort must not be null");
    }

    @Override
    public CreateOrchestrationSessionResult createSession(CreateOrchestrationSessionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String requesterGoogleSub = normalizeGoogleSub(command.requesterGoogleSub());
        requireExistingMember(requesterGoogleSub);
        requireJoinedWorkspace(command.workspaceId(), requesterGoogleSub);
        requireSourceTask(command.workspaceId(), command.sourceTaskId());
        // 스텝마다 단건 DB 조회(2N 쿼리)를 수행하지 않도록 활성 Agent 목록을 1회 적재해 O(1) 색인으로 검증한다.
        Map<Long, SavedWorkspaceAgent> activeAgentsById = indexActiveAgents(command.workspaceId());
        command.steps().forEach(step -> requireReadyAgent(command.workspaceId(), step.agentId(), activeAgentsById));

        List<OrchestrationStep> steps = command.steps().stream()
                .map(this::toDomainStep)
                .toList();
        OrchestrationSession session = OrchestrationSession.create(
                command.workspaceId(),
                command.sourceTaskId(),
                requesterGoogleSub,
                steps);

        return OrchestrationResultMapper.toCreateResult(saveOrchestrationSessionPort.save(session));
    }

    private OrchestrationStep toDomainStep(OrchestrationStepCommand command) {
        return OrchestrationStep.create(command.stepKey(), command.instruction(), command.agentId(), command.dependsOn());
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

    private void requireSourceTask(Long workspaceId, Long sourceTaskId) {
        loadTaskPort
                .findByWorkspaceIdAndTaskId(workspaceId, sourceTaskId)
                .orElseThrow(() -> new IllegalArgumentException("source task not found"));
    }

    private Map<Long, SavedWorkspaceAgent> indexActiveAgents(Long workspaceId) {
        List<SavedWorkspaceAgent> activeAgents = loadWorkspaceAgentPort.findActiveByWorkspaceId(workspaceId);
        if (activeAgents == null || activeAgents.isEmpty()) {
            return Map.of();
        }
        Map<Long, SavedWorkspaceAgent> indexed = new HashMap<>(activeAgents.size());
        for (SavedWorkspaceAgent agent : activeAgents) {
            if (agent != null && agent.agentId() != null) {
                indexed.putIfAbsent(agent.agentId(), agent);
            }
        }
        return indexed;
    }

    private void requireReadyAgent(
            Long workspaceId,
            Long agentId,
            Map<Long, SavedWorkspaceAgent> activeAgentsById) {
        Optional.ofNullable(activeAgentsById.get(agentId))
                .or(() -> loadWorkspaceAgentPort.findActiveByWorkspaceIdAndAgentId(workspaceId, agentId))
                .filter(agent -> agent.status() == WorkspaceAgentStatus.READY)
                .orElseThrow(() -> new IllegalArgumentException("ready workspace agent not found"));
    }

    private String normalizeGoogleSub(String googleSub) {
        if (googleSub == null || googleSub.isBlank()) {
            throw new IllegalArgumentException("requesterGoogleSub must not be blank");
        }
        return googleSub.trim();
    }
}
