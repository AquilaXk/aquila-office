package com.aquilaoffice.adapter.out.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.aquilaoffice.application.port.out.WorkspaceExecutionLock;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockCommand;
import org.junit.jupiter.api.Test;

class InMemoryWorkspaceExecutionLockAdapterTest {

    @Test
    void tryAcquireRejectsSameWorkspacePathUntilLockIsClosed() {
        InMemoryWorkspaceExecutionLockAdapter adapter = new InMemoryWorkspaceExecutionLockAdapter();

        WorkspaceExecutionLock firstLock = adapter.tryAcquire(command(301L, ".aquila/workspaces/101"))
                .orElseThrow();

        assertThat(adapter.tryAcquire(command(302L, ".aquila/workspaces/101"))).isEmpty();
        assertThat(adapter.tryAcquire(command(303L, ".aquila/workspaces/102"))).isPresent();

        firstLock.close();

        assertThat(adapter.tryAcquire(command(304L, ".aquila/workspaces/101"))).isPresent();
    }

    @Test
    void tryAcquireNormalizesWorkspacePath() {
        InMemoryWorkspaceExecutionLockAdapter adapter = new InMemoryWorkspaceExecutionLockAdapter();

        WorkspaceExecutionLock firstLock = adapter.tryAcquire(command(301L, " .aquila/workspaces/101 "))
                .orElseThrow();

        assertThat(adapter.tryAcquire(command(302L, ".aquila/workspaces/101"))).isEmpty();

        firstLock.close();
    }

    private static WorkspaceExecutionLockCommand command(Long taskId, String workspacePath) {
        return new WorkspaceExecutionLockCommand(101L, taskId, 501L, workspacePath);
    }
}
