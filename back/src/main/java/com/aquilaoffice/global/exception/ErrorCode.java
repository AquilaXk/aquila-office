package com.aquilaoffice.global.exception;

import org.springframework.http.HttpStatus;

/**
 * API 오류 응답에 필요한 HTTP status, 코드, 메시지를 제공하는 공통 계약이다.
 */
public interface ErrorCode {

    HttpStatus status();

    String code();

    String message();
}
