package com.mindmate.mindmate_server.global.exception;

public interface ErrorCode {
    String name();
    int getStatus();
    String getMessage();
}
