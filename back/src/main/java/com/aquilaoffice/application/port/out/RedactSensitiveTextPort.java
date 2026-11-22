package com.aquilaoffice.application.port.out;

/**
 * 로그나 저장 오류 메시지에서 민감한 인증 자격증명(API key, JWT, URL 비밀번호 등)을 가린다.
 */
public interface RedactSensitiveTextPort {

    String redact(String text);
}
