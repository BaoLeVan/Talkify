package com.talkify.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.talkify.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("[AppException] status={} code={} message={}", errorCode.getStatus().value(), errorCode.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("[AppException] status={} code={} message={}", errorCode.getStatus().value(), errorCode.getCode(), ex.getMessage());
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(
                        errorCode.getStatus().value(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOthers(Exception ex) {
        log.error("[Exception] message={}", ex.getMessage(), ex);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.error(
                        500,
                        "An unexpected error occurred"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("[MethodArgumentNotValidException] message={}", ex.getMessage());
        return ResponseEntity
                .status(400)
                .body(ApiResponse.error(
                        400,
                        "Invalid request parameters"
                ));
    }
}