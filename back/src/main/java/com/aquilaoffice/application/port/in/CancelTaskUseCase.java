package com.aquilaoffice.application.port.in;

/**
 * 비터미널 Task와 활성 실행을 취소 상태로 전이하는 use case다.
 */
public interface CancelTaskUseCase {

    UpdateTaskStatusResult cancelTask(CancelTaskCommand command);
}
