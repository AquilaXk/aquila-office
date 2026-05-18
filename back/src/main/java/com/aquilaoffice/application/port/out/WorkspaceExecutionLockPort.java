package com.aquilaoffice.application.port.out;

import java.util.Optional;

public interface WorkspaceExecutionLockPort {

    Optional<WorkspaceExecutionLock> tryAcquire(WorkspaceExecutionLockCommand command);

    static WorkspaceExecutionLockPort alwaysAvailable() {
        return command -> Optional.of(() -> {});
    }
}
