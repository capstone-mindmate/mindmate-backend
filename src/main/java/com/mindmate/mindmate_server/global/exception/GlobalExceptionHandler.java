package com.mindmate.mindmate_server.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponse> handleCustomException(
            CustomException e,
            HttpServletRequest request
    ) {
        log.error("CustomException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(
                e.getErrorCode(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(response);
    }

    /**
     * @Valid 어노테이션 검증 실패 시
     * 여기로 자동 라우팅
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.error("Validation error: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(
                CommonErrorCode.INVALID_INPUT,
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
