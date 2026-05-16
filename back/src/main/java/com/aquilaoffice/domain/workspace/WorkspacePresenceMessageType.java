package com.aquilaoffice.domain.workspace;

/**
 * presence SSE event type 값을 한 곳에서 관리한다.
 */
public enum WorkspacePresenceMessageType {
    SNAPSHOT("presence.snapshot"),
    JOIN("presence.join"),
    LEAVE("presence.leave"),
    POSITION("presence.position"),
    ERROR("presence.error");

    private final String value;

    WorkspacePresenceMessageType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
