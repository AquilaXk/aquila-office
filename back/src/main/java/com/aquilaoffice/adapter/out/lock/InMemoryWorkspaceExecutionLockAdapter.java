package com.aquilaoffice.adapter.out.lock;

import com.aquilaoffice.application.port.out.WorkspaceExecutionLock;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockCommand;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockPort;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * 단일 애플리케이션 인스턴스 안에서 workspacePath별 실행 중복을 막는 lock adapter다.
 */
@Component
public class InMemoryWorkspaceExecutionLockAdapter implements WorkspaceExecutionLockPort {

    private final ConcurrentMap<String, Long> runningTaskIdsByWorkspacePath = new ConcurrentHashMap<>();

    @Override
    public Optional<WorkspaceExecutionLock> tryAcquire(WorkspaceExecutionLockCommand command) {
        Long existingTaskId = runningTaskIdsByWorkspacePath.putIfAbsent(command.workspacePath(), command.taskId());
        if (existingTaskId != null) {
            return Optional.empty();
        }
        return Optional.of(new InMemoryWorkspaceExecutionLock(command.workspacePath(), command.taskId()));
    }

    private class InMemoryWorkspaceExecutionLock implements WorkspaceExecutionLock {
        private final String workspacePath;
        private final Long taskId;

        private InMemoryWorkspaceExecutionLock(String workspacePath, Long taskId) {
            this.workspacePath = workspacePath;
            this.taskId = taskId;
        }

        @Override
        public void close() {
            runningTaskIdsByWorkspacePath.remove(workspacePath, taskId);
        }
    }
}
