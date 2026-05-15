package com.aquilaoffice.global.handler;

import com.aquilaoffice.global.exception.CommonErrorCode;
import com.aquilaoffice.global.exception.ErrorCode;
import com.aquilaoffice.global.exception.ServiceException;
import com.aquilaoffice.global.response.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handleServiceException(ServiceException exception) {
        return error(exception.errorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleValidationException() {
        return error(CommonErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleException() {
        return error(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<RsData<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status())
                .body(RsData.fail(errorCode.code(), errorCode.message()));
    }
}
