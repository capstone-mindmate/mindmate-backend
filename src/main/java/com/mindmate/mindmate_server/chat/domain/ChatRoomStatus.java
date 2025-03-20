package com.mindmate.mindmate_server.chat.domain;

import lombok.Getter;

@Getter
public enum ChatRoomStatus {
    // todo: 매칭방 상황에 따른 status 설정도 해야함
    ACTIVE,
    PENDING,
    CLOSED,
    DELETED
}
