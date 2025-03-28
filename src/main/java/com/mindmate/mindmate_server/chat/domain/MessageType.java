package com.mindmate.mindmate_server.chat.domain;

import lombok.Getter;

@Getter
public enum MessageType {
    TEXT,
    IMAGE,
    STICKER,
    EMOTICON,
    CUSTOM_FORM,
    SYSTEM // 나중에 토스트 박스?
}
