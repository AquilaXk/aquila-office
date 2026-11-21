package com.aquilaoffice.adapter.out.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.agent.WorkspaceAgentPersistenceAdapter;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.DeleteWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.RecoverStaleTasksPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.SaveWorkspaceAgentPort;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.application.port.out.WorkspaceMemberTaskStatusCount;
import com.aquilaoffice.domain.agent.WorkspaceAgent;
import com.aquilaoffice.domain.agent.WorkspaceAgentCategory;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.Workspace;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Task persistence adapter가 workspace 범위로 Task를 저장, 조회, 상태 변경하는지 검증한다.
 */
@DataJpaTest
@Import({TaskPersistenceAdapter.class, WorkspaceAgentPersistenceAdapter.class})
class TaskPersistenceAdapterTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    @Autowired
    private SaveTaskPort saveTaskPort;

    @Autowired
    private LoadTasksPort loadTasksPort;

    @Autowired
    private LoadTaskPort loadTaskPort;

    @Autowired
    private UpdateTaskStatusPort updateTaskStatusPort;

    @Autowired
    private RecoverStaleTasksPort recoverStaleTasksPort;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SaveWorkspaceAgentPort saveWorkspaceAgentPort;

    @Autowired
    private DeleteWorkspaceAgentPort deleteWorkspaceAgentPort;

    @Test
    void savePersistsTaskThenLoadsListAndDetail() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");

        var saved = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                " creator-google ",
                " 테스트 작성 ",
                " 서비스 테스트 추가 ",
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                "T123:C456:1715841000.000100"));

        assertThat(saved.taskId()).isNotNull();
        assertThat(saved.title()).isEqualTo("테스트 작성");
        assertThat(saved.status()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(saved.sourceRef()).isEqualTo("T123:C456:1715841000.000100");

        assertThat(loadTasksPort.findByWorkspaceId(workspaceEntity.getId()))
                .singleElement()
                .satisfies(task -> {
                    assertThat(task.taskId()).isEqualTo(saved.taskId());
                    assertThat(task.assignedAgentId()).isEqualTo(501L);
                });

        assertThat(loadTaskPort.findByWorkspaceIdAndTaskId(workspaceEntity.getId(), saved.taskId()))
                .hasValueSatisfying(task -> {
                    assertThat(task.description()).isEqualTo("서비스 테스트 추가");
                    assertThat(task.priority()).isEqualTo(TaskPriority.HIGH);
                    assertThat(task.sourceRef()).isEqualTo("T123:C456:1715841000.000100");
                });
    }

    @Test
    void updateStatusChangesWorkspaceScopedTask() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var saved = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "구현",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.MEDIUM,
                null));

        var updated = updateTaskStatusPort.updateStatus(
                new TaskStatusUpdate(workspaceEntity.getId(), saved.taskId(), TaskStatus.IN_PROGRESS));

        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(taskJpaRepository.findById(saved.taskId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    void countGroupedByAgentOwnerAndStatusAggregatesActiveAgentsOnly() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("owner-a");
        memberJpaRepository.save(MemberJpaEntity.create(
                "owner-b", "owner-b@example.com", "Owner B", MemberRole.USER));

        var agentA = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(workspaceEntity.getId(), "Agent A", WorkspaceAgentCategory.BACKEND, null, "owner-a"),
                List.of());
        var agentB = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(workspaceEntity.getId(), "Agent B", WorkspaceAgentCategory.QA, null, "owner-b"),
                List.of());
        var disabledAgent = saveWorkspaceAgentPort.save(
                WorkspaceAgent.create(workspaceEntity.getId(), "Disabled", WorkspaceAgentCategory.CUSTOM, null, "owner-a"),
                List.of());
        deleteWorkspaceAgentPort.disable(workspaceEntity.getId(), disabledAgent.agentId());

        var completedTask = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "owner-a",
                "완료 1",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                agentA.agentId()));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                workspaceEntity.getId(), completedTask.taskId(), TaskStatus.COMPLETED));
        var completedTask2 = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "owner-a",
                "완료 2",
                null,
                TaskType.CODE_REVIEW,
                TaskPriority.MEDIUM,
                agentA.agentId()));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                workspaceEntity.getId(), completedTask2.taskId(), TaskStatus.COMPLETED));
        saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "owner-b",
                "할당 1",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.LOW,
                agentB.agentId()));
        // 비활성 에이전트 작업과 미할당 작업은 집계에서 제외된다.
        saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "owner-a",
                "비활성 에이전트 작업",
                null,
                TaskType.OTHER,
                TaskPriority.LOW,
                disabledAgent.agentId()));
        saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "owner-a",
                "미할당 작업",
                null,
                TaskType.OTHER,
                TaskPriority.LOW,
                null));

        assertThat(loadTasksPort.countGroupedByAgentOwnerAndStatus(workspaceEntity.getId()))
                .hasValueSatisfying(counts -> assertThat(counts).containsExactlyInAnyOrder(
                        new WorkspaceMemberTaskStatusCount("owner-a", TaskStatus.COMPLETED, 2L),
                        new WorkspaceMemberTaskStatusCount("owner-b", TaskStatus.ASSIGNED, 1L)));
    }

    @Test
    void saveRejectsMissingWorkspaceEntity() {
        assertThatThrownBy(() -> saveTaskPort.save(Task.create(
                        404L,
                        "google-1",
                        "구현",
                        null,
                        TaskType.FEATURE_IMPLEMENTATION,
                        TaskPriority.MEDIUM,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("workspace entity not found");
    }

    @Test
    void recoverStaleTasksMarksOnlyOldInProgressTasksAsFailed() {
        WorkspaceJpaEntity workspaceEntity = saveWorkspaceEntity("creator-google");
        var staleTask = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "고착 작업",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                null));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                workspaceEntity.getId(), staleTask.taskId(), TaskStatus.IN_PROGRESS));

        var freshTask = saveTaskPort.save(Task.create(
                workspaceEntity.getId(),
                "creator-google",
                "최근 작업",
                null,
                TaskType.FEATURE_IMPLEMENTATION,
                TaskPriority.HIGH,
                null));
        updateTaskStatusPort.updateStatus(new TaskStatusUpdate(
                workspaceEntity.getId(), freshTask.taskId(), TaskStatus.IN_PROGRESS));

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        entityManager.flush();
        entityManager.createNativeQuery("update tasks set updated_at = :oldTime where id = :id")
                .setParameter("oldTime", nowUtc.minusMinutes(30))
                .setParameter("id", staleTask.taskId())
                .executeUpdate();
        entityManager.clear();

        int recovered = recoverStaleTasksPort.recoverStaleTasks(
                workspaceEntity.getId(),
                nowUtc.minusMinutes(15),
                nowUtc);

        assertThat(recovered).isEqualTo(1);
        assertThat(taskJpaRepository.findById(staleTask.taskId()).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.FAILED);
        assertThat(taskJpaRepository.findById(freshTask.taskId()).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.IN_PROGRESS);
    }

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
