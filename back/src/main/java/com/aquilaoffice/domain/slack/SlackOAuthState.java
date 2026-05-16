package com.aquilaoffice.domain.slack;

/**
 * Slack OAuth callback에서 workspace와 requester를 복원하기 위한 state 값이다.
 */
public record SlackOAuthState(
        Long workspaceId,
        String requesterGoogleSub) {

    private static final String WORKSPACE_PREFIX = "workspaceId";
    private static final String REQUESTER_PREFIX = "requester";
    private static final int STATE_PARTS = 4;

    public SlackOAuthState {
        workspaceId = requirePositive(workspaceId);
        requesterGoogleSub = requireNotBlank(requesterGoogleSub);
    }

    public static SlackOAuthState parse(String value) {
        String normalizedValue = requireRawState(value);
        String[] parts = normalizedValue.split(":", -1);
        if (parts.length != STATE_PARTS
                || !WORKSPACE_PREFIX.equals(parts[0])
                || !REQUESTER_PREFIX.equals(parts[2])) {
            throw new IllegalArgumentException("slack oauth state is invalid");
        }
        return new SlackOAuthState(parseWorkspaceId(parts[1]), parts[3]);
    }

    public static String serialize(Long workspaceId, String requesterGoogleSub) {
        SlackOAuthState state = new SlackOAuthState(workspaceId, requesterGoogleSub);
        return WORKSPACE_PREFIX + ":" + state.workspaceId()
                + ":" + REQUESTER_PREFIX + ":" + state.requesterGoogleSub();
    }

    private static String requireRawState(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("slack oauth state is invalid");
        }
        return value.trim();
    }

    private static Long parseWorkspaceId(String value) {
        try {
            return requirePositive(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("slack oauth state workspaceId must be positive", exception);
        }
    }

    private static Long requirePositive(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("slack oauth state workspaceId must be positive");
        }
        return value;
    }

    private static String requireNotBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("slack oauth state requesterGoogleSub must not be blank");
        }
        return value.trim();
    }
}
