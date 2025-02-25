package com.mindmate.mindmate_server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
    // 채팅방 관련 오류
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    CHAT_ROOM_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 채팅방입니다"),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅방에 접근 권한이 없습니다"),
    CHAT_ROOM_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "채팅방 생성에 실패했습니다"),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 채팅방입니다"),

    // 메시지 관련 오류
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다"),
    MESSAGE_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지 전송에 실패했습니다"),
    MESSAGE_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "메시지 내용이 비어있습니다"),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지 길이가 제한을 초과했습니다"),
    MESSAGE_CONTAINS_INAPPROPRIATE_CONTENT(HttpStatus.BAD_REQUEST, "부적절한 내용이 포함되어 있습니다"),

    // 사용자 참여 관련 오류
    USER_ALREADY_IN_CHAT(HttpStatus.CONFLICT, "이미 채팅방에 참여 중입니다"),
    USER_NOT_IN_CHAT(HttpStatus.FORBIDDEN, "채팅방에 참여하지 않은 사용자입니다"),
    USER_BLOCKED(HttpStatus.FORBIDDEN, "차단된 사용자와는 채팅할 수 없습니다"),
    INVALID_ROLE_FOR_CHAT(HttpStatus.BAD_REQUEST, "현재 역할로는 이 채팅에 참여할 수 없습니다"),

    // 읽음 상태 관련 오류
    READ_STATUS_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "읽음 상태 업데이트에 실패했습니다"),

    // 연결 관련 오류
    WEBSOCKET_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WebSocket 연결에 실패했습니다"),
    REDIS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 연결에 실패했습니다"),
    KAFKA_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Kafka 연결에 실패했습니다"),

    // 알림 관련 오류
    NOTIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송에 실패했습니다");

    private final HttpStatus status;
    private final String message;
}
