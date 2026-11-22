package com.aquilaoffice.application.service;

import com.aquilaoffice.domain.workspace.WorkspacePresenceMember;
import com.aquilaoffice.domain.workspace.WorkspacePresenceMessage;
import com.aquilaoffice.domain.workspace.WorkspacePresencePosition;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 워크스페이스별 현재 멤버 위치 상태와 SSE sink를 상한·만료 정책과 함께 메모리에 보관한다.
 */
public class WorkspacePresenceRegistry {

    static final int MAX_IDLE_MEMBER_STATES_PER_ROOM = 256;
    static final int MAX_SINKS_PER_MEMBER = 4;
    static final int MAX_SINKS_PER_ROOM = 128;

    // 워크스페이스 단위로 락 경계를 분리해 다른 워크스페이스 이동 이벤트가 서로 블로킹하지 않도록 한다.
    private final ConcurrentMap<Long, RoomPresenceState> roomsByWorkspaceId = new ConcurrentHashMap<>();
    private final Clock clock;

    public WorkspacePresenceRegistry() {
        this(Clock.systemUTC());
    }

    WorkspacePresenceRegistry(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public WorkspacePresenceMessage updatePosition(
            Long workspaceId,
            Long memberId,
            WorkspacePresencePosition position,
            String status) {
        Long normalizedWorkspaceId = requirePositive(workspaceId, "workspaceId");
        Long normalizedMemberId = requirePositive(memberId, "memberId");
        WorkspacePresencePosition normalizedPosition = Objects.requireNonNull(position, "position must not be null");
        Instant now = clock.instant();

        roomsByWorkspaceId.compute(normalizedWorkspaceId, (ignored, existingRoom) -> {
            RoomPresenceState room = existingRoom != null ? existingRoom : new RoomPresenceState();
            synchronized (room) {
                WorkspacePresenceState state = touchMemberState(room, normalizedMemberId);
                state.position = normalizedPosition;
                state.status = status;
                state.lastSeenAt = now;
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
        Instant now = clock.instant();
        AtomicReference<JoinResult> resultRef = new AtomicReference<>();
        List<WorkspacePresenceEventSink> evictedSinks = new ArrayList<>();

        roomsByWorkspaceId.compute(normalizedWorkspaceId, (ignored, existingRoom) -> {
            RoomPresenceState room = existingRoom != null ? existingRoom : new RoomPresenceState();
            synchronized (room) {
                WorkspacePresenceState state = touchMemberState(room, normalizedMemberId);
                boolean becameOnline = state.sinks.isEmpty();
                evictOverflowMemberSinks(state, normalizedSink, evictedSinks);
                state.sinks.put(normalizedSink, now);
                state.lastSeenAt = now;
                evictOverflowRoomSinks(room, normalizedSink, evictedSinks);
                evictExcessIdleMembers(room, normalizedMemberId);
                resultRef.set(new JoinResult(
                        WorkspacePresenceMessage.snapshot(normalizedWorkspaceId, snapshotMembers(room)),
                        becameOnline ? WorkspacePresenceMessage.join(normalizedWorkspaceId, normalizedMemberId) : null));
            }
            return room;
        });

        for (WorkspacePresenceEventSink evicted : evictedSinks) {
            evicted.closeSilently();
        }
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
                for (WorkspacePresenceEventSink sink : entry.getValue().sinks.keySet()) {
                    connections.add(new WorkspacePresenceConnection(normalizedWorkspaceId, memberId, sink));
                }
            }
            return List.copyOf(connections);
        }
    }

    public List<WorkspacePresenceConnection> snapshotAllConnections() {
        List<WorkspacePresenceConnection> connections = new ArrayList<>();
        for (Long workspaceId : roomsByWorkspaceId.keySet()) {
            connections.addAll(connectionsInWorkspace(workspaceId));
        }
        return List.copyOf(connections);
    }

    public List<WorkspacePresenceMessage> evictStaleConnections(Duration maxIdleDuration) {
        Objects.requireNonNull(maxIdleDuration, "maxIdleDuration must not be null");
        Instant cutoff = clock.instant().minus(maxIdleDuration);
        List<WorkspacePresenceEventSink> staleSinks = new ArrayList<>();
        List<WorkspacePresenceMessage> leaveMessages = new ArrayList<>();

        for (Long workspaceId : List.copyOf(roomsByWorkspaceId.keySet())) {
            roomsByWorkspaceId.computeIfPresent(workspaceId, (ignored, room) -> {
                synchronized (room) {
                    Iterator<Map.Entry<Long, WorkspacePresenceState>> memberIter = room.members.entrySet().iterator();
                    while (memberIter.hasNext()) {
                        Map.Entry<Long, WorkspacePresenceState> memberEntry = memberIter.next();
                        Long memberId = memberEntry.getKey();
                        WorkspacePresenceState state = memberEntry.getValue();
                        boolean hadSinks = !state.sinks.isEmpty();

                        Iterator<Map.Entry<WorkspacePresenceEventSink, Instant>> sinkIter =
                                state.sinks.entrySet().iterator();
                        while (sinkIter.hasNext()) {
                            Map.Entry<WorkspacePresenceEventSink, Instant> sinkEntry = sinkIter.next();
                            if (sinkEntry.getValue().isBefore(cutoff)) {
                                staleSinks.add(sinkEntry.getKey());
                                sinkIter.remove();
                            }
                        }

                        if (hadSinks && state.sinks.isEmpty()) {
                            memberIter.remove();
                            leaveMessages.add(WorkspacePresenceMessage.leave(workspaceId, memberId));
                        } else if (state.sinks.isEmpty() && state.lastSeenAt.isBefore(cutoff)) {
                            memberIter.remove();
                        }
                    }
                    return room.members.isEmpty() ? null : room;
                }
            });
        }

        for (WorkspacePresenceEventSink sink : staleSinks) {
            sink.closeSilently();
        }
        return List.copyOf(leaveMessages);
    }

    public void touchSinkHeartbeat(Long workspaceId, Long memberId, WorkspacePresenceEventSink sink) {
        RoomPresenceState room = roomsByWorkspaceId.get(workspaceId);
        if (room == null) {
            return;
        }
        Instant now = clock.instant();
        synchronized (room) {
            WorkspacePresenceState state = room.members.get(memberId);
            if (state != null && state.sinks.containsKey(sink)) {
                state.sinks.put(sink, now);
                state.lastSeenAt = now;
            }
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
        WorkspacePresenceState state = existing != null ? existing : new WorkspacePresenceState(clock.instant());
        room.members.put(memberId, state);
        return state;
    }

    private void evictOverflowMemberSinks(
            WorkspacePresenceState state,
            WorkspacePresenceEventSink incomingSink,
            List<WorkspacePresenceEventSink> evictedOut) {
        if (state.sinks.containsKey(incomingSink)) {
            return;
        }
        while (state.sinks.size() >= MAX_SINKS_PER_MEMBER) {
            Iterator<Map.Entry<WorkspacePresenceEventSink, Instant>> iterator = state.sinks.entrySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            evictedOut.add(iterator.next().getKey());
            iterator.remove();
        }
    }

    private void evictOverflowRoomSinks(
            RoomPresenceState room,
            WorkspacePresenceEventSink protectedSink,
            List<WorkspacePresenceEventSink> evictedOut) {
        int totalSinks = countRoomSinks(room);
        while (totalSinks > MAX_SINKS_PER_ROOM) {
            if (!evictOldestRoomSink(room, protectedSink, evictedOut)) {
                break;
            }
            totalSinks--;
        }
    }

    private static int countRoomSinks(RoomPresenceState room) {
        int total = 0;
        for (WorkspacePresenceState state : room.members.values()) {
            total += state.sinks.size();
        }
        return total;
    }

    private static boolean evictOldestRoomSink(
            RoomPresenceState room,
            WorkspacePresenceEventSink protectedSink,
            List<WorkspacePresenceEventSink> evictedOut) {
        Iterator<Map.Entry<Long, WorkspacePresenceState>> memberIter = room.members.entrySet().iterator();
        while (memberIter.hasNext()) {
            WorkspacePresenceState state = memberIter.next().getValue();
            Iterator<Map.Entry<WorkspacePresenceEventSink, Instant>> sinkIter = state.sinks.entrySet().iterator();
            while (sinkIter.hasNext()) {
                WorkspacePresenceEventSink candidate = sinkIter.next().getKey();
                if (candidate == protectedSink) {
                    continue;
                }
                sinkIter.remove();
                evictedOut.add(candidate);
                if (state.sinks.isEmpty()) {
                    memberIter.remove();
                }
                return true;
            }
        }
        return false;
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
        private final Map<WorkspacePresenceEventSink, Instant> sinks = new LinkedHashMap<>();
        private Instant lastSeenAt;
        private WorkspacePresencePosition position;
        private String status;

        private WorkspacePresenceState(Instant now) {
            this.lastSeenAt = now;
        }
    }
}
