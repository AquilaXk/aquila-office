package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteCommand;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.AcceptWorkspaceInviteUseCase;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteQuery;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteResult;
import com.aquilaoffice.application.port.in.GetWorkspaceInviteUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import com.aquilaoffice.global.security.AuthenticatedMember;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 초대 링크 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping({"/api/v1/workspace-invites", "/api/v1/invites"})
public class WorkspaceInviteController {

    private final GetWorkspaceInviteUseCase getWorkspaceInviteUseCase;
    private final AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase;

    public WorkspaceInviteController(
            GetWorkspaceInviteUseCase getWorkspaceInviteUseCase,
            AcceptWorkspaceInviteUseCase acceptWorkspaceInviteUseCase) {
        this.getWorkspaceInviteUseCase = getWorkspaceInviteUseCase;
        this.acceptWorkspaceInviteUseCase = acceptWorkspaceInviteUseCase;
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

    @PostMapping("/{token}/accept")
    public ResponseEntity<RsData<AcceptWorkspaceInviteResponse>> acceptWorkspaceInvite(
            @PathVariable String token,
            Authentication authentication) {
        String normalizedMemberGoogleSub = requireAuthenticatedMemberGoogleSub(authentication);

        try {
            AcceptWorkspaceInviteResult result = acceptWorkspaceInviteUseCase.acceptWorkspaceInvite(
                    new AcceptWorkspaceInviteCommand(token, normalizedMemberGoogleSub));
            return ResponseEntity.ok(RsData.ok(AcceptWorkspaceInviteResponse.from(result)));
        } catch (IllegalArgumentException exception) {
            // token 수락 실패 사유는 초대 존재 여부와 멤버십 정보를 드러내지 않도록 동일한 404로 응답한다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireAuthenticatedMemberGoogleSub(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedMember authenticatedMember)) {
            throw new ServiceException(CommonErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember.googleSub();
    }
}
