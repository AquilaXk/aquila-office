package com.aquilaoffice.global.handler;

import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 발생한 예외를 공통 응답 포맷으로 변환하는 전역 예외 핸들러다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handleServiceException(ServiceException exception) {
        return error(exception.errorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleValidationException() {
        // 아직 필드별 오류 계약이 없으므로 API 공통 BAD_REQUEST 코드로 통일한다.
        return error(CommonErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handleUnreadableMessageException() {
        return error(CommonErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException() {
        return error(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<RsData<Void>> error(ErrorCode errorCode) {
        // HTTP status와 body code/message는 ErrorCode 한 곳에서 관리해 응답 일관성을 유지한다.
        return ResponseEntity.status(errorCode.status())
                .body(RsData.fail(errorCode.code(), errorCode.message()));
    }
}
