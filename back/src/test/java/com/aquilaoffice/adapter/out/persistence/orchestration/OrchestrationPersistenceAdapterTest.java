package com.aquilaoffice.adapter.out.persistence.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.OrchestrationSessionStatusUpdate;
import com.aquilaoffice.application.port.out.OrchestrationStepTaskUpdate;
import com.aquilaoffice.application.port.out.SaveOrchestrationSessionPort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationSessionStatusPort;
import com.aquilaoffice.application.port.out.UpdateOrchestrationStepPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.orchestration.OrchestrationSession;
import com.aquilaoffice.domain.orchestration.OrchestrationSessionStatus;
import com.aquilaoffice.domain.orchestration.OrchestrationStep;
import com.aquilaoffice.domain.orchestration.OrchestrationStepStatus;
import com.aquilaoffice.domain.workspace.Workspace;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(OrchestrationPersistenceAdapter.class)
class OrchestrationPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private OrchestrationSessionJpaRepository orchestrationSessionJpaRepository;

    @Autowired
    private OrchestrationStepJpaRepository orchestrationStepJpaRepository;

    @Autowired
    private SaveOrchestrationSessionPort saveOrchestrationSessionPort;

    @Autowired
    private LoadOrchestrationSessionPort loadOrchestrationSessionPort;

    @Autowired
    private UpdateOrchestrationStepPort updateOrchestrationStepPort;

    @Autowired
    private UpdateOrchestrationSessionStatusPort updateOrchestrationSessionStatusPort;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveAndLoadSessionWithOrderedStepsAndDependencies() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        var saved = saveOrchestrationSessionPort.save(OrchestrationSession.create(
                workspaceEntity.getId(),
                301L,
                "creator-google",
                List.of(
                        OrchestrationStep.create("plan", "계획 정리", 501L, List.of()),
                        OrchestrationStep.create("backend", "서비스 구현", 502L, List.of("plan")))));

        assertThat(saved.sessionId()).isNotNull();
        assertThat(orchestrationSessionJpaRepository.count()).isEqualTo(1);
        assertThat(orchestrationStepJpaRepository.count()).isEqualTo(2);

        assertThat(loadOrchestrationSessionPort.findByWorkspaceIdAndSessionId(
                        workspaceEntity.getId(),
                        saved.sessionId()))
                .hasValueSatisfying(session -> {
                    assertThat(session.getStatus()).isEqualTo(OrchestrationSessionStatus.PLANNED);
                    assertThat(session.getSteps()).extracting(OrchestrationStep::getStepKey)
                            .containsExactly("plan", "backend");
                    assertThat(session.getSteps().get(1).getDependsOn()).containsExactly("plan");
                });
    }

    @Test
    void updateStepTaskAndSessionStatus() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var saved = saveOrchestrationSessionPort.save(OrchestrationSession.create(
                workspaceEntity.getId(),
                301L,
                "creator-google",
                List.of(OrchestrationStep.create("plan", "계획 정리", 501L, List.of()))));

        var updatedStep = updateOrchestrationStepPort.updateStep(new OrchestrationStepTaskUpdate(
                workspaceEntity.getId(),
                saved.sessionId(),
                "plan",
                401L,
                OrchestrationStepStatus.SUCCEEDED));
        var updatedSession = updateOrchestrationSessionStatusPort.updateStatus(new OrchestrationSessionStatusUpdate(
                workspaceEntity.getId(),
                saved.sessionId(),
                OrchestrationSessionStatus.SUCCEEDED));

        assertThat(updatedStep.taskId()).isEqualTo(401L);
        assertThat(updatedStep.status()).isEqualTo(OrchestrationStepStatus.SUCCEEDED);
        assertThat(updatedSession.status()).isEqualTo(OrchestrationSessionStatus.SUCCEEDED);
        assertThat(loadOrchestrationSessionPort.findByWorkspaceIdAndSessionId(
                        workspaceEntity.getId(),
                        saved.sessionId()))
                .hasValueSatisfying(session -> {
                    assertThat(session.getStatus()).isEqualTo(OrchestrationSessionStatus.SUCCEEDED);
                    assertThat(session.getSteps().getFirst().getTaskId()).isEqualTo(401L);
                    assertThat(session.getSteps().getFirst().getStatus())
                            .isEqualTo(OrchestrationStepStatus.SUCCEEDED);
                });
    }

    @Test
    void findByWorkspaceIdOrderByIdDescBatchLoadsStepsWithoutNPlusOne() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var first = saveOrchestrationSessionPort.save(OrchestrationSession.create(
                workspaceEntity.getId(),
                301L,
                "creator-google",
                List.of(
                        OrchestrationStep.create("plan", "계획", 501L, List.of()),
                        OrchestrationStep.create("impl", "구현", 502L, List.of("plan")))));
        var second = saveOrchestrationSessionPort.save(OrchestrationSession.create(
                workspaceEntity.getId(),
                302L,
                "creator-google",
                List.of(OrchestrationStep.create("review", "리뷰", 503L, List.of()))));

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        var listed = loadOrchestrationSessionPort.findByWorkspaceIdOrderByIdDesc(workspaceEntity.getId());

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2L);
        assertThat(listed).extracting("sessionId").containsExactly(second.sessionId(), first.sessionId());
        assertThat(listed.get(0).steps()).extracting("stepKey").containsExactly("review");
        assertThat(listed.get(1).steps()).extracting("stepKey").containsExactly("plan", "impl");
        assertThat(loadOrchestrationSessionPort.findDetailByWorkspaceIdAndSessionId(
                        workspaceEntity.getId(), first.sessionId()))
                .hasValueSatisfying(detail -> {
                    assertThat(detail.createdAt()).isNotNull();
                    assertThat(detail.updatedAt()).isNotNull();
                    assertThat(detail.steps()).hasSize(2);
                });
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub,
                googleSub + "@example.com",
                "Creator",
                MemberRole.USER));
        Workspace workspace = Workspace.create(
                "Team",
                "Description",
                Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
