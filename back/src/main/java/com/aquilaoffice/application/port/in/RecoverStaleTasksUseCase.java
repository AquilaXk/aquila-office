package com.aquilaoffice.application.port.in;

/**
 * 워크스페이스의 고착된 Task와 TaskExecution을 FAILED로 복구하는 use case다.
 */
public interface RecoverStaleTasksUseCase {

    RecoverStaleTasksResult recoverStaleTasks(RecoverStaleTasksCommand command);
}
