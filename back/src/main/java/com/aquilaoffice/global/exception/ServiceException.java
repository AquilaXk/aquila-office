package com.aquilaoffice.global.exception;

/**
 * application 또는 adapter 계층에서 의도한 API 오류를 표현하는 runtime exception이다.
 */
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
