package com.aquilaoffice.domain.workspace;

import java.util.List;

/**
 * presence SSE로 전달되는 공통 event payload다.
 */
public record WorkspacePresenceMessage(
        String type,
        Long workspaceId,
        Long memberId,
        WorkspacePresencePosition position,
        String status,
        List<WorkspacePresenceMember> members,
        String message) {

    public WorkspacePresenceMessage {
        type = requireNotBlank(type, "type");
        workspaceId = requirePositive(workspaceId, "workspaceId");
        members = members == null ? List.of() : List.copyOf(members);
    }

    public static WorkspacePresenceMessage snapshot(Long workspaceId, List<WorkspacePresenceMember> members) {
        return new WorkspacePresenceMessage(
                WorkspacePresenceMessageType.SNAPSHOT.value(),
                workspaceId,
                null,
                null,
                null,
                members,
                null);
    }

    public static WorkspacePresenceMessage join(Long workspaceId, Long memberId) {
        return memberEvent(WorkspacePresenceMessageType.JOIN, workspaceId, memberId);
    }

    public static WorkspacePresenceMessage leave(Long workspaceId, Long memberId) {
        return memberEvent(WorkspacePresenceMessageType.LEAVE, workspaceId, memberId);
    }

    public static WorkspacePresenceMessage position(
            Long workspaceId,
            Long memberId,
            WorkspacePresencePosition position,
            String status) {
        return new WorkspacePresenceMessage(
                WorkspacePresenceMessageType.POSITION.value(),
                workspaceId,
                requirePositive(memberId, "memberId"),
                position,
                status,
                List.of(),
                null);
    }

    public static WorkspacePresenceMessage error(Long workspaceId, String message) {
        return new WorkspacePresenceMessage(
                WorkspacePresenceMessageType.ERROR.value(),
                workspaceId,
                null,
                null,
                null,
                List.of(),
                message);
    }

    private static WorkspacePresenceMessage memberEvent(
            WorkspacePresenceMessageType type,
            Long workspaceId,
            Long memberId) {
        return new WorkspacePresenceMessage(
                type.value(),
                workspaceId,
                requirePositive(memberId, "memberId"),
                null,
                null,
                List.of(),
                null);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
