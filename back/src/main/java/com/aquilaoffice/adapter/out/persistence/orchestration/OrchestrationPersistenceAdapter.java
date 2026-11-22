package com.aquilaoffice.adapter.out.persistence.orchestration;

import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.OrchestrationSessionStatusUpdate;
import com.aquilaoffice.application.port.out.OrchestrationStepTaskUpdate;
import com.aquilaoffice.application.port.out.SaveOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.SavedOrchestrationSession;
import com.aquilaoffice.application.port.out.SavedOrchestrationStep;
import com.aquilaoffice.application.port.out.UpdateOrchestrationSessionStatusPort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationStepPort;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration session/step 저장과 step dispatch 상태 갱신을 담당하는 persistence adapter다.
 */
@Repository
public class OrchestrationPersistenceAdapter
        implements SaveOrchestrationSessionPort,
                LoadOrchestrationSessionPort,
                UpdateOrchestrationStepPort,
                UpdateOrchestrationSessionStatusPort {

    private final WorkspaceJpaRepository workspaceJpaRepository;
    private final OrchestrationSessionJpaRepository orchestrationSessionJpaRepository;
    private final OrchestrationStepJpaRepository orchestrationStepJpaRepository;

    public OrchestrationPersistenceAdapter(
            WorkspaceJpaRepository workspaceJpaRepository,
            OrchestrationSessionJpaRepository orchestrationSessionJpaRepository,
            OrchestrationStepJpaRepository orchestrationStepJpaRepository) {
        this.workspaceJpaRepository = workspaceJpaRepository;
        this.orchestrationSessionJpaRepository = orchestrationSessionJpaRepository;
        this.orchestrationStepJpaRepository = orchestrationStepJpaRepository;
    }

    @Override
    @Transactional
    public SavedOrchestrationSession save(OrchestrationSession session) {
        Objects.requireNonNull(session, "session must not be null");
        WorkspaceJpaEntity workspaceEntity = workspaceJpaRepository
                .findActiveWorkspaceById(session.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("workspace entity not found"));
        return toSavedSession(orchestrationSessionJpaRepository.save(
                OrchestrationSessionJpaEntity.from(session, workspaceEntity)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrchestrationSession> findByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return orchestrationSessionJpaRepository.findByWorkspace_IdAndId(workspaceId, sessionId)
                .map(this::toDomain);
    }

    private static final int MAX_SESSIONS_PAGE_SIZE = 200;

    @Override
    @Transactional(readOnly = true)
    public List<SavedOrchestrationSession> findByWorkspaceIdOrderByIdDesc(Long workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        List<OrchestrationSessionJpaEntity> sessions =
                orchestrationSessionJpaRepository.findByWorkspace_IdOrderByIdDesc(
                        workspaceId,
                        PageRequest.of(0, MAX_SESSIONS_PAGE_SIZE));
        if (sessions.isEmpty()) {
            return List.of();
        }
        List<Long> sessionIds = sessions.stream()
                .map(OrchestrationSessionJpaEntity::getId)
                .toList();
        // 세션 수만큼 step 쿼리가 반복되지 않도록 session_id IN 배치 조회 후 메모리에서 1패스로 매핑한다.
        List<OrchestrationStepJpaEntity> steps =
                orchestrationStepJpaRepository.findBySession_IdInOrderBySession_IdAscSortOrderAsc(sessionIds);
        Map<Long, List<SavedOrchestrationStep>> stepsBySessionId = new HashMap<>(sessionIds.size() * 2);
        for (OrchestrationStepJpaEntity step : steps) {
            stepsBySessionId
                    .computeIfAbsent(step.getSession().getId(), ignored -> new ArrayList<>())
                    .add(toSavedStep(step));
        }
        return sessions.stream()
                .map(entity -> new SavedOrchestrationSession(
                        entity.getId(),
                        entity.getWorkspace().getId(),
                        entity.getSourceTaskId(),
                        entity.getCreatedByGoogleSub(),
                        entity.getStatus(),
                        stepsBySessionId.getOrDefault(entity.getId(), List.of()),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SavedOrchestrationSession> findDetailByWorkspaceIdAndSessionId(Long workspaceId, Long sessionId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return orchestrationSessionJpaRepository.findByWorkspace_IdAndId(workspaceId, sessionId)
                .map(this::toSavedSession);
    }

    @Override
    @Transactional
    public SavedOrchestrationStep updateStep(OrchestrationStepTaskUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        OrchestrationStepJpaEntity step = orchestrationStepJpaRepository
                .findBySession_Workspace_IdAndSession_IdAndStepKey(
                        update.workspaceId(),
                        update.sessionId(),
                        update.stepKey())
                .orElseThrow(() -> new IllegalArgumentException("orchestration step not found"));
        step.updateTask(update.taskId(), update.status());
        return toSavedStep(step);
    }

    @Override
    @Transactional
    public SavedOrchestrationSession updateStatus(OrchestrationSessionStatusUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        OrchestrationSessionJpaEntity session = orchestrationSessionJpaRepository
                .findByWorkspace_IdAndId(update.workspaceId(), update.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("orchestration session not found"));
        session.updateStatus(update.status());
        return toSavedSession(session);
    }

    private OrchestrationSession toDomain(OrchestrationSessionJpaEntity entity) {
        return OrchestrationSession.restore(
                entity.getId(),
                entity.getWorkspace().getId(),
                entity.getSourceTaskId(),
                entity.getCreatedByGoogleSub(),
                entity.getStatus(),
                entity.getSteps().stream()
                        .map(this::toDomainStep)
                        .toList());
    }

    private OrchestrationStep toDomainStep(OrchestrationStepJpaEntity entity) {
        return OrchestrationStep.restore(
                entity.getId(),
                entity.getStepKey(),
                entity.getInstruction(),
                entity.getAgentId(),
                entity.getTaskId(),
                entity.getDependsOn(),
                entity.getStatus());
    }

    private SavedOrchestrationSession toSavedSession(OrchestrationSessionJpaEntity entity) {
        return new SavedOrchestrationSession(
                entity.getId(),
                entity.getWorkspace().getId(),
                entity.getSourceTaskId(),
                entity.getCreatedByGoogleSub(),
                entity.getStatus(),
                entity.getSteps().stream().map(this::toSavedStep).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private SavedOrchestrationStep toSavedStep(OrchestrationStepJpaEntity entity) {
        return new SavedOrchestrationStep(
                entity.getId(),
                entity.getStepKey(),
                entity.getInstruction(),
                entity.getAgentId(),
                entity.getTaskId(),
                entity.getDependsOn(),
                entity.getStatus());
    }
}
