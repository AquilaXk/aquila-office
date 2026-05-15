package com.aquilaoffice.application.port.out;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * persistence adapter가 초대 수락과 멤버 추가를 함께 처리할 때 사용하는 요청값이다.
 */
public record WorkspaceInviteAcceptance(
        Long inviteId,
        Long workspaceId,
        String memberGoogleSub,
        WorkspaceMemberRole role,
        LocalDateTime acceptedAt) {}
