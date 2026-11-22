package com.aquilaoffice.global.security;

import com.aquilaoffice.application.port.out.RedactSensitiveTextPort;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * O(N) 단일 패스 스캐너로 Bearer 토큰, 공급자 API 키, JWT, URL 자격증명을 마스킹한다.
 * 정규표현식 역추적(ReDoS)을 피하기 위해 선형 문자 스캔만 수행한다.
 */
@Component
public class SecretRedactor implements RedactSensitiveTextPort {

    private static final String REDACTED = "[REDACTED]";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MIN_SK_TOKEN_LEN = 12;
    private static final int MIN_GOOGLE_KEY_LEN = 16;
    private static final int MIN_PREFIX_KEY_LEN = 10;
    private static final List<String> TOKEN_PREFIXES = List.of(
            "sk-ant-",
            "sk-proj-",
            "sk-svcacct-",
            "sk-",
            "xai-",
            "gsk_",
            "ghp_",
            "gho_",
            "ghu_",
            "ghs_",
            "ghr_",
            "github_pat_",
            "xoxb-",
            "xoxp-",
            "xoxa-",
            "xoxr-",
            "xoxe.");

    @Override
    public String redact(String text) {
        return redactText(text);
    }

    public static String redactThrowable(Throwable throwable, int maxLength) {
        if (throwable == null) {
            return "";
        }
        String raw = throwable.getMessage();
        if (raw == null || raw.isBlank()) {
            raw = throwable.getClass().getSimpleName();
        }
        String sanitized = redactText(raw);
        if (maxLength > 0 && sanitized.length() > maxLength) {
            return sanitized.substring(0, maxLength);
        }
        return sanitized;
    }

    public static String redactText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        int len = text.length();
        StringBuilder out = null;
        int copiedUntil = 0;
        int i = 0;

        while (i < len) {
            int matchEnd = matchUrlCredentialsEnd(text, i);
            if (matchEnd > i) {
                out = ensureBuilder(out, text, copiedUntil, i + 3);
                out.append(REDACTED);
                copiedUntil = matchEnd;
                i = matchEnd;
                continue;
            }

            if (matchesIgnoreCaseAt(text, i, BEARER_PREFIX)) {
                int tokenStart = i + BEARER_PREFIX.length();
                while (tokenStart < len && text.charAt(tokenStart) == ' ') {
                    tokenStart++;
                }
                int tokenEnd = scanNonWhitespace(text, tokenStart);
                if (tokenEnd > tokenStart) {
                    out = ensureBuilder(out, text, copiedUntil, i);
                    out.append(BEARER_PREFIX).append(REDACTED);
                    copiedUntil = tokenEnd;
                    i = tokenEnd;
                    continue;
                }
            }

            int jwtEnd = matchJwtEnd(text, i);
            if (jwtEnd > i) {
                out = ensureBuilder(out, text, copiedUntil, i);
                out.append(REDACTED);
                copiedUntil = jwtEnd;
                i = jwtEnd;
                continue;
            }

            int apiKeyEnd = matchApiKeyEnd(text, i);
            if (apiKeyEnd > i) {
                out = ensureBuilder(out, text, copiedUntil, i);
                out.append(REDACTED);
                copiedUntil = apiKeyEnd;
                i = apiKeyEnd;
                continue;
            }

            i++;
        }

        if (out == null) {
            return text;
        }
        out.append(text, copiedUntil, len);
        return out.toString();
    }

    private static StringBuilder ensureBuilder(StringBuilder out, String text, int copiedUntil, int current) {
        StringBuilder builder = out != null ? out : new StringBuilder(text.length());
        builder.append(text, copiedUntil, current);
        return builder;
    }

    private static int matchUrlCredentialsEnd(String text, int index) {
        if (!text.startsWith("://", index)) {
            return -1;
        }
        int authorityStart = index + 3;
        int colonPos = -1;
        int len = text.length();
        for (int pos = authorityStart; pos < len; pos++) {
            char ch = text.charAt(pos);
            if (ch == '@') {
                if (colonPos > authorityStart) {
                    return pos; // redact up to '@', keep '@' and host
                }
                return -1;
            }
            if (ch == ':' && colonPos < 0) {
                colonPos = pos;
            } else if (ch == '/' || ch == '?' || ch == '#' || Character.isWhitespace(ch)) {
                return -1;
            }
        }
        return -1;
    }

    private static int matchJwtEnd(String text, int index) {
        if (!isTokenBoundaryBefore(text, index) || !text.startsWith("eyJ", index)) {
            return -1;
        }
        int len = text.length();
        int seg1End = scanBase64Url(text, index);
        if (seg1End - index < 6 || seg1End >= len || text.charAt(seg1End) != '.') {
            return -1;
        }
        int seg2Start = seg1End + 1;
        int seg2End = scanBase64Url(text, seg2Start);
        if (seg2End - seg2Start < 6 || seg2End >= len || text.charAt(seg2End) != '.') {
            return -1;
        }
        int seg3Start = seg2End + 1;
        int seg3End = scanBase64Url(text, seg3Start);
        if (seg3End - seg3Start < 6) {
            return -1;
        }
        return seg3End;
    }

    private static int matchApiKeyEnd(String text, int index) {
        if (!isTokenBoundaryBefore(text, index)) {
            return -1;
        }
        if (text.startsWith("AIza", index)) {
            int end = scanTokenChars(text, index);
            return (end - index >= MIN_GOOGLE_KEY_LEN) ? end : -1;
        }
        for (String prefix : TOKEN_PREFIXES) {
            if (text.startsWith(prefix, index)) {
                int end = scanTokenChars(text, index);
                int minLen = "sk-".equals(prefix)
                        ? prefix.length() + MIN_SK_TOKEN_LEN
                        : prefix.length() + MIN_PREFIX_KEY_LEN;
                return (end - index >= minLen) ? end : -1;
            }
        }
        return -1;
    }

    private static boolean isTokenBoundaryBefore(String text, int index) {
        if (index == 0) {
            return true;
        }
        char prev = text.charAt(index - 1);
        return !(Character.isLetterOrDigit(prev) || prev == '_');
    }

    private static int scanNonWhitespace(String text, int start) {
        int pos = start;
        int len = text.length();
        while (pos < len) {
            char ch = text.charAt(pos);
            if (Character.isWhitespace(ch) || ch == '"' || ch == '\'' || ch == ',' || ch == ';') {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static int scanTokenChars(String text, int start) {
        int pos = start;
        int len = text.length();
        while (pos < len) {
            char ch = text.charAt(pos);
            if (!(Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.')) {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static int scanBase64Url(String text, int start) {
        int pos = start;
        int len = text.length();
        while (pos < len) {
            char ch = text.charAt(pos);
            if (!(Character.isLetterOrDigit(ch) || ch == '-' || ch == '_')) {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static boolean matchesIgnoreCaseAt(String text, int index, String prefix) {
        int prefixLen = prefix.length();
        if (index + prefixLen > text.length()) {
            return false;
        }
        return text.regionMatches(true, index, prefix, 0, prefixLen);
    }
}
