package com.aquilaoffice.adapter.out.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aquilaoffice.adapter.out.persistence.member.MemberJpaEntity;
import com.aquilaoffice.adapter.out.persistence.member.MemberJpaRepository;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaEntity;
import com.aquilaoffice.adapter.out.persistence.workspace.WorkspaceJpaRepository;
import com.aquilaoffice.application.port.out.LoadTaskPort;
import com.aquilaoffice.application.port.out.LoadTasksPort;
import com.aquilaoffice.application.port.out.SaveTaskPort;
import com.aquilaoffice.application.port.out.TaskStatusUpdate;
import com.aquilaoffice.application.port.out.UpdateTaskStatusPort;
import com.aquilaoffice.domain.member.Member;
import com.aquilaoffice.domain.member.MemberRole;
import com.aquilaoffice.domain.task.Task;
import com.aquilaoffice.domain.task.TaskPriority;
import com.aquilaoffice.domain.task.TaskStatus;
import com.aquilaoffice.domain.task.TaskType;
import com.aquilaoffice.domain.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Task persistence adapter가 workspace 범위로 Task를 저장, 조회, 상태 변경하는지 검증한다.
 */
@DataJpaTest
@Import(TaskPersistenceAdapter.class)
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
                501L));

        assertThat(saved.taskId()).isNotNull();
        assertThat(saved.title()).isEqualTo("테스트 작성");
        assertThat(saved.status()).isEqualTo(TaskStatus.ASSIGNED);

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

    private WorkspaceJpaEntity saveWorkspaceEntity(String googleSub) {
        MemberJpaEntity creatorEntity = memberJpaRepository.save(MemberJpaEntity.create(
                googleSub, googleSub + "@example.com", "Creator", MemberRole.USER));
        Workspace workspace =
                Workspace.create("Team", "Description", Member.createUser(googleSub, googleSub + "@example.com", "Creator"));
        return workspaceJpaRepository.save(WorkspaceJpaEntity.from(workspace, creatorEntity));
    }
}
