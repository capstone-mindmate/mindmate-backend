package com.mindmate.mindmate_server.global.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final HttpStatus status;
    private final String error;
    private final String message;
    private final String path;

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .error(errorCode.name())
                .message(errorCode.getMessage())
                .path(path)
                .build();
    }
}
