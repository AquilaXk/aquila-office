package com.aquilaoffice.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Task 도메인이 생성 입력을 정규화하고 상태 전이 규칙을 보호하는지 검증한다.
 */
class TaskTest {

    @Test
    void createDefaultsToRequestedWhenAgentIsMissing() {
        Task task = Task.create(
                101L,
                " google-1 ",
                " 코드 리뷰 ",
                " 변경 내용을 확인해주세요. ",
                null,
                null,
                null);

        assertThat(task.getWorkspaceId()).isEqualTo(101L);
        assertThat(task.getCreatedByGoogleSub()).isEqualTo("google-1");
        assertThat(task.getTitle()).isEqualTo("코드 리뷰");
        assertThat(task.getDescription()).isEqualTo("변경 내용을 확인해주세요.");
        assertThat(task.getType()).isEqualTo(TaskType.OTHER);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getAssignedAgentId()).isNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.REQUESTED);
    }

    @Test
    void createDefaultsToAssignedWhenAgentExists() {
        Task task = Task.create(
                101L,
                "google-1",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L);

        assertThat(task.getType()).isEqualTo(TaskType.TEST_CREATION);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getAssignedAgentId()).isEqualTo(501L);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
    }

    @Test
    void changeStatusAllowsForwardTransitions() {
        Task task = Task.restore(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.ASSIGNED);

        task.changeStatus(TaskStatus.IN_PROGRESS);
        task.changeStatus(TaskStatus.COMPLETED);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void changeStatusRejectsTerminalTransition() {
        Task task = Task.restore(
                301L,
                101L,
                "google-1",
                "테스트 작성",
                null,
                TaskType.TEST_CREATION,
                TaskPriority.HIGH,
                501L,
                TaskStatus.COMPLETED);

        assertThatThrownBy(() -> task.changeStatus(TaskStatus.IN_PROGRESS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("terminal task status cannot be changed");
    }

    @Test
    void createRejectsBlankTitle() {
        assertThatThrownBy(() -> Task.create(
                        101L,
                        "google-1",
                        " ",
                        null,
                        TaskType.OTHER,
                        TaskPriority.MEDIUM,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
    }

    @Test
    void changeStatusAllowsWaitingUserFromRequestedAndAssigned() {
        Task requestedTask = Task.restore(
                301L,
                101L,
                "google-1",
                "요청 작업",
                null,
                TaskType.OTHER,
                TaskPriority.MEDIUM,
                null,
                TaskStatus.REQUESTED);
        requestedTask.changeStatus(TaskStatus.WAITING_USER);
        assertThat(requestedTask.getStatus()).isEqualTo(TaskStatus.WAITING_USER);

        Task assignedTask = Task.restore(
                302L,
                101L,
                "google-1",
                "할당 작업",
                null,
                TaskType.OTHER,
                TaskPriority.MEDIUM,
                501L,
                TaskStatus.ASSIGNED);
        assignedTask.changeStatus(TaskStatus.WAITING_USER);
        assertThat(assignedTask.getStatus()).isEqualTo(TaskStatus.WAITING_USER);
        assignedTask.changeStatus(TaskStatus.IN_PROGRESS);
        assertThat(assignedTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assignedTask.changeStatus(TaskStatus.CANCELED);
        assertThat(assignedTask.getStatus()).isEqualTo(TaskStatus.CANCELED);
    }
}
