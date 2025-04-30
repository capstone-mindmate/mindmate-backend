package com.mindmate.mindmate_server.chat.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum ChatEventType {
    MESSAGE,
    READ_STATUS,
    REACTION,
    CUSTOM_FORM,
    CUSTOM_FORM_RESPONSE,
    CONTENT_FILTERED,
    USER_STATUS,
    TOAST_BOX,
    EMOTICON
}
