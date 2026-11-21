package com.aquilaoffice.application.service;

import com.aquilaoffice.domain.workspace.WorkspacePresenceMember;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 워크스페이스별 현재 멤버 위치 상태를 메모리에 보관한다.
 */
public class WorkspacePresenceRegistry {

    static final int MAX_IDLE_MEMBER_STATES_PER_ROOM = 256;

    // 워크스페이스 단위로 락 경계를 분리해 다른 워크스페이스 이동 이벤트가 서로 블로킹하지 않도록 한다.
    private final ConcurrentMap<Long, RoomPresenceState> roomsByWorkspaceId = new ConcurrentHashMap<>();

    public WorkspacePresenceMessage updatePosition(
            Long workspaceId,
            Long memberId,
            WorkspacePresencePosition position,
            String status) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresencePosition normalizedPosition = Objects.requireNonNull(position, "position must not be null");

        roomsByWorkspaceId.compute(normalizedWorkspaceId, (ignored, existingRoom) -> {
            RoomPresenceState room = existingRoom != null ? existingRoom : new RoomPresenceState();
            synchronized (room) {
                WorkspacePresenceState state = touchMemberState(room, normalizedMemberId);
                state.position = normalizedPosition;
                state.status = status;
                evictExcessIdleMembers(room, normalizedMemberId);
            }
            return room;
        });

        return WorkspacePresenceMessage.position(normalizedWorkspaceId, normalizedMemberId, normalizedPosition, status);
    }

    public JoinResult join(Long workspaceId, Long memberId, WorkspacePresenceEventSink sink) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresenceEventSink normalizedSink = Objects.requireNonNull(sink, "sink must not be null");
        AtomicReference<JoinResult> resultRef = new AtomicReference<>();

        roomsByWorkspaceId.compute(normalizedWorkspaceId, (ignored, existingRoom) -> {
            RoomPresenceState room = existingRoom != null ? existingRoom : new RoomPresenceState();
            synchronized (room) {
                WorkspacePresenceState state = touchMemberState(room, normalizedMemberId);
                boolean becameOnline = state.sinks.isEmpty();
                state.sinks.add(normalizedSink);
                evictExcessIdleMembers(room, normalizedMemberId);
                resultRef.set(new JoinResult(
                        WorkspacePresenceMessage.snapshot(normalizedWorkspaceId, snapshotMembers(room)),
                        becameOnline ? WorkspacePresenceMessage.join(normalizedWorkspaceId, normalizedMemberId) : null));
            }
            return room;
        });

        return resultRef.get();
    }

    public LeaveResult leave(Long workspaceId, Long memberId, WorkspacePresenceEventSink sink) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresenceEventSink normalizedSink = Objects.requireNonNull(sink, "sink must not be null");
        AtomicReference<LeaveResult> resultRef = new AtomicReference<>(new LeaveResult(null));

        roomsByWorkspaceId.computeIfPresent(normalizedWorkspaceId, (ignored, room) -> {
            synchronized (room) {
                WorkspacePresenceState state = room.members.get(normalizedMemberId);
                if (state == null) {
                    return room.members.isEmpty() ? null : room;
                }

                state.sinks.remove(normalizedSink);
                if (!state.sinks.isEmpty()) {
                    return room;
                }

                room.members.remove(normalizedMemberId);
                resultRef.set(new LeaveResult(
                        WorkspacePresenceMessage.leave(normalizedWorkspaceId, normalizedMemberId)));
                return room.members.isEmpty() ? null : room;
            }
        });

        return resultRef.get();
    }

    public List<WorkspacePresenceConnection> connectionsInWorkspace(Long workspaceId) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        RoomPresenceState room = roomsByWorkspaceId.get(normalizedWorkspaceId);
        if (room == null) {
            return List.of();
        }

        synchronized (room) {
            if (room.members.isEmpty()) {
                return List.of();
            }
            List<WorkspacePresenceConnection> connections = new ArrayList<>();
            for (Map.Entry<Long, WorkspacePresenceState> entry : room.members.entrySet()) {
                Long memberId = entry.getKey();
                for (WorkspacePresenceEventSink sink : entry.getValue().sinks) {
                    connections.add(new WorkspacePresenceConnection(normalizedWorkspaceId, memberId, sink));
                }
            }
            return List.copyOf(connections);
        }
    }

    public Optional<MemberState> findMemberState(Long workspaceId, Long memberId) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");

        RoomPresenceState room = roomsByWorkspaceId.get(normalizedWorkspaceId);
        if (room == null) {
            return Optional.empty();
        }
        synchronized (room) {
            WorkspacePresenceState state = room.members.get(normalizedMemberId);
            if (state == null || state.position == null) {
                return Optional.empty();
            }
            return Optional.of(new MemberState(state.position, state.status));
        }
    }

    private WorkspacePresenceState touchMemberState(RoomPresenceState room, Long memberId) {
        WorkspacePresenceState existing = room.members.remove(memberId);
        WorkspacePresenceState state = existing != null ? existing : new WorkspacePresenceState();
        room.members.put(memberId, state);
        return state;
    }

    private void evictExcessIdleMembers(RoomPresenceState room, Long protectedMemberId) {
        int idleCount = 0;
        for (WorkspacePresenceState state : room.members.values()) {
            if (state.sinks.isEmpty()) {
                idleCount++;
            }
        }
        if (idleCount <= MAX_IDLE_MEMBER_STATES_PER_ROOM) {
            return;
        }
        Iterator<Map.Entry<Long, WorkspacePresenceState>> iterator = room.members.entrySet().iterator();
        while (iterator.hasNext() && idleCount > MAX_IDLE_MEMBER_STATES_PER_ROOM) {
            Map.Entry<Long, WorkspacePresenceState> entry = iterator.next();
            if (entry.getKey().equals(protectedMemberId)) {
                continue;
            }
            if (entry.getValue().sinks.isEmpty()) {
                iterator.remove();
                idleCount--;
            }
        }
    }

    private List<WorkspacePresenceMember> snapshotMembers(RoomPresenceState room) {
        if (room.members.isEmpty()) {
            return List.of();
        }
        List<WorkspacePresenceMember> snapshot = new ArrayList<>(room.members.size());
        for (Map.Entry<Long, WorkspacePresenceState> entry : room.members.entrySet()) {
            WorkspacePresenceState state = entry.getValue();
            snapshot.add(new WorkspacePresenceMember(entry.getKey(), state.position, state.status));
        }
        snapshot.sort(Comparator.comparing(WorkspacePresenceMember::memberId));
        return List.copyOf(snapshot);
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

    private static final class RoomPresenceState {
        private final Map<Long, WorkspacePresenceState> members = new LinkedHashMap<>();
    }

    private static final class WorkspacePresenceState {
        private final Set<WorkspacePresenceEventSink> sinks = new LinkedHashSet<>();
        private WorkspacePresencePosition position;
        private String status;
    }
}
