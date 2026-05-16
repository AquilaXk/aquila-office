package com.aquilaoffice.application.port.out;

public record SlackReplyRequest(
        String sourceRef,
        String teamId,
        String channelId,
        String threadTs,
        String text,
        String deduplicationKey) {

    public static SlackReplyRequest fromSourceRef(String sourceRef, String text, String deduplicationKey) {
        String normalizedSourceRef = requireNotBlank(sourceRef, "sourceRef");
        String[] parts = normalizedSourceRef.split(":", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("sourceRef must be formatted as teamId:channelId:threadTs");
        }
        return new SlackReplyRequest(
                normalizedSourceRef,
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                requireNotBlank(text, "text"),
                normalizeOptional(deduplicationKey));
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
