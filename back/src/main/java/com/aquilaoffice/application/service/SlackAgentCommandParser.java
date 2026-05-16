package com.aquilaoffice.application.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SlackAgentCommandParser {

    private static final Pattern SLACK_MENTION_PATTERN = Pattern.compile("<@[A-Z0-9]+>");
    private static final Pattern AGENT_COMMAND_PATTERN =
            Pattern.compile("^/agent\\s+(\\d+)(?:\\s+(.*))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    ParsedCommand parse(String text) {
        String sanitizedText = sanitizeText(text);
        if (sanitizedText.isBlank()) {
            return new ParsedCommand(false, null, "");
        }

        Matcher matcher = AGENT_COMMAND_PATTERN.matcher(sanitizedText);
        if (!matcher.matches()) {
            return new ParsedCommand(false, null, sanitizedText);
        }

        Long agentId = Long.valueOf(matcher.group(1));
        String message = matcher.group(2) == null ? "" : matcher.group(2).strip();
        return new ParsedCommand(!message.isBlank(), agentId, message);
    }

    private String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return SLACK_MENTION_PATTERN.matcher(text).replaceAll("").strip();
    }

    record ParsedCommand(
            boolean matched,
            Long agentId,
            String message) {}
}
