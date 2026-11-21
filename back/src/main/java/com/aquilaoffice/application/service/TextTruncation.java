package com.aquilaoffice.application.service;

/**
 * DB VARCHAR 컬럼 상한에 맞춰 UTF-16 서로게이트 쌍 경계를 훼손하지 않고 문자열을 절단한다.
 */
final class TextTruncation {

    private TextTruncation() {}

    static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0 || value.length() <= maxLength) {
            return value;
        }
        int endIndex = maxLength;
        if (endIndex > 0
                && Character.isHighSurrogate(value.charAt(endIndex - 1))
                && Character.isLowSurrogate(value.charAt(endIndex))) {
            endIndex--;
        }
        return value.substring(0, endIndex);
    }
}
