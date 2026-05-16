package com.aquilaoffice.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * TaskExecution 도메인이 실행 기록의 필수 식별자와 상태 전이를 보호하는지 검증한다.
 */
class TaskExecutionTest {

    @Test
    void createStartsWithQueuedStatus() {
        TaskExecution execution = TaskExecution.create(101L, 301L, 501L);

        assertThat(execution.getWorkspaceId()).isEqualTo(101L);
        assertThat(execution.getTaskId()).isEqualTo(301L);
        assertThat(execution.getAgentId()).isEqualTo(501L);
        assertThat(execution.getStatus()).isEqualTo(TaskExecutionStatus.QUEUED);
        assertThat(execution.getResultSummary()).isNull();
        assertThat(execution.getFailureReason()).isNull();
    }

    @Test
    void markRunningAndSucceededUpdatesStatusAndSummary() {
        TaskExecution execution = TaskExecution.create(101L, 301L, 501L);

        execution.markRunning();
        execution.markSucceeded(" 작업 완료 ");

        assertThat(execution.getStatus()).isEqualTo(TaskExecutionStatus.SUCCEEDED);
        assertThat(execution.getResultSummary()).isEqualTo("작업 완료");
        assertThat(execution.getFailureReason()).isNull();
    }

    @Test
    void markFailedStoresReason() {
        TaskExecution execution = TaskExecution.create(101L, 301L, 501L);

        execution.markFailed(" gateway timeout ");

        assertThat(execution.getStatus()).isEqualTo(TaskExecutionStatus.FAILED);
        assertThat(execution.getFailureReason()).isEqualTo("gateway timeout");
    }

    @Test
    void createRejectsMissingAgent() {
        assertThatThrownBy(() -> TaskExecution.create(101L, 301L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("agentId must be positive");
    }
}
