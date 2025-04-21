package com.mindmate.mindmate_server.chat.domain;

import lombok.Getter;

@Getter
public enum ChatRoomStatus {
    ACTIVE, // 대화 중
    PENDING, // 매칭 잡는 중
    CLOSED, // 대화 끝
    CLOSE_REQUEST, // 종료 요청
    DELETED
}
