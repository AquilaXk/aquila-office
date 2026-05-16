package com.aquilaoffice.application.service;

import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 워크스페이스별 현재 멤버 위치 상태를 메모리에 보관한다.
 */
public class WorkspacePresenceRegistry {

    private final Map<Long, Map<Long, MemberState>> workspaces = new HashMap<>();

    public synchronized void updatePosition(
            Long workspaceId,
            Long memberId,
            WorkspacePresencePosition position,
            String status) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresencePosition normalizedPosition = Objects.requireNonNull(position, "position must not be null");

        workspaces.computeIfAbsent(normalizedWorkspaceId, ignored -> new HashMap<>())
                .put(normalizedMemberId, new MemberState(normalizedPosition, status));
    }

    public synchronized Optional<MemberState> findMemberState(Long workspaceId, Long memberId) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");

        Map<Long, MemberState> members = workspaces.get(normalizedWorkspaceId);
        if (members == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(members.get(normalizedMemberId));
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    public record MemberState(WorkspacePresencePosition position, String status) {

        public MemberState {
            position = Objects.requireNonNull(position, "position must not be null");
        }
    }
}
