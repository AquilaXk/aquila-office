package com.aquilaoffice.application.port.in;

/**
 * Task를 사용자 승인 대기(WAITING_USER) 상태로 전환하는 use case다.
 */
public interface RequestTaskApprovalUseCase {

    UpdateTaskStatusResult requestApproval(RequestTaskApprovalCommand command);
}
