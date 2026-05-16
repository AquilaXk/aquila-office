package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.UpdateWorkspacePresencePositionUseCase;
import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 워크스페이스 presence 관련 HTTP 요청을 application use case로 연결하는 inbound web adapter다.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/presence")
public class WorkspacePresenceController {

    private final UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase;

    public WorkspacePresenceController(UpdateWorkspacePresencePositionUseCase updateWorkspacePresencePositionUseCase) {
        this.updateWorkspacePresencePositionUseCase = updateWorkspacePresencePositionUseCase;
    }

    @PatchMapping("/me/position")
    public ResponseEntity<RsData<Void>> updateMyPosition(
            @PathVariable Long workspaceId,
            @RequestHeader(name = WorkspaceController.CREATOR_GOOGLE_SUB_HEADER, required = false)
                    String memberGoogleSub,
            @Valid @RequestBody(required = false) WorkspacePresencePositionUpdateRequest request) {
        String normalizedMemberGoogleSub = requireMemberGoogleSub(memberGoogleSub);
        if (request == null) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }

        try {
            updateWorkspacePresencePositionUseCase.updatePosition(
                    request.toCommand(workspaceId, normalizedMemberGoogleSub));
            return ResponseEntity.ok(RsData.ok(null));
        } catch (IllegalArgumentException exception) {
            // presence 상태는 가입된 워크스페이스 멤버에게만 노출하므로 미가입/미존재를 같은 404로 숨긴다.
            throw new ServiceException(CommonErrorCode.NOT_FOUND);
        }
    }

    private String requireMemberGoogleSub(String memberGoogleSub) {
        if (memberGoogleSub == null || memberGoogleSub.isBlank()) {
            throw new ServiceException(CommonErrorCode.BAD_REQUEST);
        }
        return memberGoogleSub.trim();
    }
}
