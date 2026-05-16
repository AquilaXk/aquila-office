package com.aquilaoffice.application.service;

import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMember;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 워크스페이스별 현재 멤버 위치 상태를 메모리에 보관한다.
 */
public class WorkspacePresenceRegistry {

    private final Map<Long, WorkspacePresenceRoom> workspaces = new HashMap<>();

    public synchronized WorkspacePresenceMessage updatePosition(
            Long workspaceId,
            Long memberId,
            WorkspacePresencePosition position,
            String status) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresencePosition normalizedPosition = Objects.requireNonNull(position, "position must not be null");

        WorkspacePresenceRoom room = workspaces.computeIfAbsent(
                normalizedWorkspaceId,
                ignored -> new WorkspacePresenceRoom());
        WorkspacePresenceState state = room.members.computeIfAbsent(
                normalizedMemberId,
                ignored -> new WorkspacePresenceState());
        state.position = normalizedPosition;
        state.status = status;

        return WorkspacePresenceMessage.position(normalizedWorkspaceId, normalizedMemberId, normalizedPosition, status);
    }

    public synchronized JoinResult join(Long workspaceId, Long memberId, WorkspacePresenceEventSink sink) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresenceEventSink normalizedSink = Objects.requireNonNull(sink, "sink must not be null");

        WorkspacePresenceRoom room = workspaces.computeIfAbsent(
                normalizedWorkspaceId,
                ignored -> new WorkspacePresenceRoom());
        WorkspacePresenceState state = room.members.computeIfAbsent(
                normalizedMemberId,
                ignored -> new WorkspacePresenceState());

        boolean becameOnline = state.sinks.isEmpty();
        state.sinks.add(normalizedSink);

        return new JoinResult(
                WorkspacePresenceMessage.snapshot(normalizedWorkspaceId, snapshotMembers(room)),
                becameOnline ? WorkspacePresenceMessage.join(normalizedWorkspaceId, normalizedMemberId) : null);
    }

    public synchronized LeaveResult leave(Long workspaceId, Long memberId, WorkspacePresenceEventSink sink) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresenceEventSink normalizedSink = Objects.requireNonNull(sink, "sink must not be null");

        WorkspacePresenceRoom room = workspaces.get(normalizedWorkspaceId);
        if (room == null) {
            return new LeaveResult(null);
        }
        WorkspacePresenceState state = room.members.get(normalizedMemberId);
        if (state == null) {
            return new LeaveResult(null);
        }

        state.sinks.remove(normalizedSink);
        if (!state.sinks.isEmpty()) {
            return new LeaveResult(null);
        }

        room.members.remove(normalizedMemberId);
        if (room.members.isEmpty()) {
            workspaces.remove(normalizedWorkspaceId);
        }
        return new LeaveResult(WorkspacePresenceMessage.leave(normalizedWorkspaceId, normalizedMemberId));
    }

    public synchronized List<WorkspacePresenceConnection> connectionsInWorkspace(Long workspaceId) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        WorkspacePresenceRoom room = workspaces.get(normalizedWorkspaceId);
        if (room == null) {
            return List.of();
        }

        return room.members.entrySet().stream()
                .flatMap(entry -> entry.getValue().sinks.stream()
                        .map(sink -> new WorkspacePresenceConnection(normalizedWorkspaceId, entry.getKey(), sink)))
                .toList();
    }

    public synchronized Optional<MemberState> findMemberState(Long workspaceId, Long memberId) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");

        WorkspacePresenceRoom room = workspaces.get(normalizedWorkspaceId);
        if (room == null) {
            return Optional.empty();
        }
        WorkspacePresenceState state = room.members.get(normalizedMemberId);
        if (state == null || state.position == null) {
            return Optional.empty();
        }
        return Optional.of(new MemberState(state.position, state.status));
    }

    private List<WorkspacePresenceMember> snapshotMembers(WorkspacePresenceRoom room) {
        return room.members.entrySet().stream()
                .map(entry -> new WorkspacePresenceMember(
                        entry.getKey(),
                        entry.getValue().position,
                        entry.getValue().status))
                .sorted(Comparator.comparing(WorkspacePresenceMember::memberId))
                .toList();
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

    public record JoinResult(WorkspacePresenceMessage snapshot, WorkspacePresenceMessage joinEvent) {}

    public record LeaveResult(WorkspacePresenceMessage leaveEvent) {}

    public record WorkspacePresenceConnection(Long workspaceId, Long memberId, WorkspacePresenceEventSink sink) {}

    private static class WorkspacePresenceRoom {
        private final Map<Long, WorkspacePresenceState> members = new HashMap<>();
    }

    private static class WorkspacePresenceState {
        private final Set<WorkspacePresenceEventSink> sinks = new LinkedHashSet<>();
        private WorkspacePresencePosition position;
        private String status;
    }
}
