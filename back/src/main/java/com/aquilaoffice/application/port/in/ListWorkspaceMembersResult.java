package com.aquilaoffice.application.port.in;

import com.aquilaoffice.domain.workspace.WorkspaceMemberRole;
import java.time.LocalDateTime;

/**
 * 워크스페이스 멤버 목록 조회 결과를 adapter와 독립된 application 값으로 표현한다.
 */
public record ListWorkspaceMembersResult(
        Long memberId,
        String memberGoogleSub,
        String email,
        String name,
        WorkspaceMemberRole role,
        LocalDateTime joinedAt) {}
