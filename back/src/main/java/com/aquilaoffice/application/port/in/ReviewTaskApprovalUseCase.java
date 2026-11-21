package com.aquilaoffice.application.port.in;

/**
 * 사용자 승인 대기(WAITING_USER) Task를 승인 또는 반려하는 use case다.
 */
public interface ReviewTaskApprovalUseCase {

    UpdateTaskStatusResult reviewApproval(ReviewTaskApprovalCommand command);
}
