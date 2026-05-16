package com.aquilaoffice.adapter.in.web.workspace;

import com.aquilaoffice.application.port.in.ExtendWorkspaceInviteCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 워크스페이스 초대 만료일 연장 HTTP 요청 body다.
 */
public record ExtendWorkspaceInviteRequest(@Min(1) @Max(30) Integer additionalDays) {

    ExtendWorkspaceInviteCommand toCommand(Long workspaceId, Long inviteId, String requesterGoogleSub) {
        return new ExtendWorkspaceInviteCommand(workspaceId, inviteId, requesterGoogleSub, additionalDays);
    }
}

