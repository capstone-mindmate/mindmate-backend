package com.mindmate.mindmate_server.chat.domain;

import lombok.Getter;

@Getter
public enum ChatRoomCloseType {
    REQUEST,
    ACCEPT,
    REJECT
}
