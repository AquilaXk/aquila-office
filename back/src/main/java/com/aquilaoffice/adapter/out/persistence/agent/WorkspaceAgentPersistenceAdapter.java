package com.aquilaoffice.adapter.out.persistence.agent;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgent;
import com.aquilaoffice.application.port.out.SavedWorkspaceAgentSkillFile;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentSkillFile;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent outbound port를 JPA 저장소로 구현하는 persistence adapter다.
 */
@Repository
public class WorkspaceAgentPersistenceAdapter
        implements SaveWorkspaceAgentPort, LoadWorkspaceAgentPort, DeleteWorkspaceAgentPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final WorkspaceAgentJpaRepository workspaceAgentJpaRepository;
    private final WorkspaceAgentSkillFileJpaRepository workspaceAgentSkillFileJpaRepository;

    public WorkspaceAgentPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            WorkspaceAgentJpaRepository workspaceAgentJpaRepository,
            WorkspaceAgentSkillFileJpaRepository workspaceAgentSkillFileJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.workspaceAgentJpaRepository = workspaceAgentJpaRepository;
        this.workspaceAgentSkillFileJpaRepository = workspaceAgentSkillFileJpaRepository;
    }

    @Override
    @Transactional
    public SavedWorkspaceAgent save(WorkspaceAgent agent, List<WorkspaceAgentSkillFile> skillFiles) {
        Objects.requireNonNull(agent, "agent must not be null");
        List<WorkspaceAgentSkillFile> normalizedSkillFiles =
                skillFiles == null ? List.of() : List.copyOf(skillFiles);
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(agent.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));

        WorkspaceAgentJpaEntity agentEntity =
                workspaceAgentJpaRepository.save(WorkspaceAgentJpaEntity.from(agent, workspaceEntity));
        List<WorkspaceAgentSkillFileJpaEntity> skillFileEntities = normalizedSkillFiles.stream()
                .map(skillFile -> WorkspaceAgentSkillFileJpaEntity.from(skillFile, agentEntity))
                .map(workspaceAgentSkillFileJpaRepository::save)
                .toList();

        return toSavedAgent(agentEntity, skillFileEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedWorkspaceAgent> findActiveByWorkspaceId(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        List<WorkspaceAgentJpaEntity> agentEntities = workspaceAgentJpaRepository.findActiveByWorkspaceId(workspaceId);
        Map<Long, List<WorkspaceAgentSkillFileJpaEntity>> skillFilesByAgentId =
                findSkillFilesByAgentId(agentEntities);

        return agentEntities.stream()
                .map(agent -> toSavedAgent(agent, skillFilesByAgentId.getOrDefault(agent.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SavedWorkspaceAgent> findActiveByWorkspaceIdAndAgentId(
            Long workspaceId,
            Long agentId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");

        return workspaceAgentJpaRepository.findActiveByWorkspaceIdAndId(workspaceId, agentId)
                .map(agent -> toSavedAgent(
                        agent,
                        workspaceAgentSkillFileJpaRepository.findByAgentIdOrderByIdAsc(agent.getId())));
    }

    @Override
    @Transactional
    public void disable(Long workspaceId, Long agentId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");

        WorkspaceAgentJpaEntity agentEntity = workspaceAgentJpaRepository
                .findActiveByWorkspaceIdAndId(workspaceId, agentId)
                .orElseThrow(() -> new IllegalArgumentException("workspace agent entity not found"));
        agentEntity.disable();
    }

    private Map<Long, List<WorkspaceAgentSkillFileJpaEntity>> findSkillFilesByAgentId(
            List<WorkspaceAgentJpaEntity> agentEntities) {
        List<Long> agentIds = agentEntities.stream()
                .map(WorkspaceAgentJpaEntity::getId)
                .toList();
        if (agentIds.isEmpty()) {
            return Map.of();
        }

        return workspaceAgentSkillFileJpaRepository.findByAgentIdInOrderByAgentIdAscIdAsc(agentIds).stream()
                .collect(Collectors.groupingBy(skillFile -> skillFile.getAgent().getId()));
    }

    private SavedWorkspaceAgent toSavedAgent(
            WorkspaceAgentJpaEntity agent,
            List<WorkspaceAgentSkillFileJpaEntity> skillFiles) {
        return new SavedWorkspaceAgent(
                agent.getId(),
                agent.getWorkspace().getId(),
                agent.getName(),
                agent.getCategory(),
                agent.getOpenClawAgentId(),
                agent.getWorkspacePath(),
                agent.getStatus(),
                agent.getSyncError(),
                agent.getCreatedByGoogleSub(),
                agent.getCreatedAt(),
                agent.getUpdatedAt(),
                skillFiles.stream()
                        .map(this::toSavedSkillFile)
                        .toList());
    }

    private SavedWorkspaceAgentSkillFile toSavedSkillFile(WorkspaceAgentSkillFileJpaEntity skillFile) {
        return new SavedWorkspaceAgentSkillFile(
                skillFile.getId(),
                skillFile.getFileName(),
                skillFile.getSyncStatus(),
                skillFile.getSyncError());
    }
}
