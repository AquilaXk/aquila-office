package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.GetWorkspaceInviteQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 초대 링크 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspace-invites")
public class WorkspaceInviteController {

    private final GetWorkspaceInviteUseCase getWorkspaceInviteUseCase;

    public WorkspaceInviteController(GetWorkspaceInviteUseCase getWorkspaceInviteUseCase) {
        this.getWorkspaceInviteUseCase = getWorkspaceInviteUseCase;
    }

    @GetMapping("/{token}")
    public ResponseEntity<RsData<WorkspaceInviteResponse>> getWorkspaceInvite(@PathVariable String token) {
        try {
            GetWorkspaceInviteResult result =
                    getWorkspaceInviteUseCase.getWorkspaceInvite(new GetWorkspaceInviteQuery(token));
            return ResponseEntity.ok(RsData.ok(WorkspaceInviteResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // token은 초대 접근 권한이므로 존재 여부 외의 상세 실패 사유를 노출하지 않는다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }
}
