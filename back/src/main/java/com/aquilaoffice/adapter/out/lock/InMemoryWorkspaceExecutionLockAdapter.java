package com.aquilaoffice.adapter.out.lock;

import com.aquilaoffice.application.port.out.WorkspaceExecutionLock;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockCommand;
import com.aquilaoffice.application.port.out.WorkspaceExecutionLockPort;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * 단일 애플리케이션 인스턴스 안에서 workspaceId와 정규화 경로 기준으로 실행 중복을 막는 lock adapter다.
 */
@Component
public class InMemoryWorkspaceExecutionLockAdapter implements WorkspaceExecutionLockPort {

    private final ConcurrentMap<String, Long> runningTaskIdsByLockKey = new ConcurrentHashMap<>();

    @Override
    public Optional<WorkspaceExecutionLock> tryAcquire(WorkspaceExecutionLockCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String lockKey = lockKey(command.workspaceId(), command.workspacePath());
        Long existingTaskId = runningTaskIdsByLockKey.putIfAbsent(lockKey, command.taskId());
        if (existingTaskId != null) {
            return Optional.empty();
        }
        return Optional.of(new InMemoryWorkspaceExecutionLock(lockKey, command.taskId()));
    }

    private static String lockKey(Long workspaceId, String workspacePath) {
        String normalizedPath = Path.of(workspacePath.trim()).normalize().toString();
        return workspaceId + ":" + normalizedPath;
    }

    private class InMemoryWorkspaceExecutionLock implements WorkspaceExecutionLock {
        private final String lockKey;
        private final Long taskId;

        private InMemoryWorkspaceExecutionLock(String lockKey, Long taskId) {
            this.lockKey = lockKey;
            this.taskId = taskId;
        }

        @Override
        public void close() {
            runningTaskIdsByLockKey.remove(lockKey, taskId);
        }
    }
}
