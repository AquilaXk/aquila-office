package com.aquilaoffice.adapter.in.web.task;

import com.aquilaoffice.application.port.in.ListTaskReportsQuery;
import com.aquilaoffice.application.port.in.ListTaskReportsUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task 실행 결과 report 조회 요청을 application use case로 연결한다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tasks")
public class TaskReportController {

    private final ListTaskReportsUseCase listTaskReportsUseCase;

    public TaskReportController(ListTaskReportsUseCase listTaskReportsUseCase) {
        this.listTaskReportsUseCase = listTaskReportsUseCase;
    }

    @GetMapping("/{taskId}/reports")
    public ResponseEntity<RsData<List<TaskReportResponse>>> listTaskReports(
            @PathVariable Long workspaceId,
            @PathVariable Long taskId,
            Authentication authentication) {
        String normalizedRequesterGoogleSub = requireAuthenticatedRequesterGoogleSub(authentication);

        try {
            return ResponseEntity.ok(RsData.ok(listTaskReportsUseCase
                    .listTaskReports(new ListTaskReportsQuery(workspaceId, taskId, normalizedRequesterGoogleSub))
                    .stream()
                    .map(TaskReportResponse::from)
                    .toList()));
        } catch (IllegalArgumentException exception) {
            // Task report도 워크스페이스 멤버에게만 노출하므로 존재 여부와 권한 실패를 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireAuthenticatedRequesterGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
