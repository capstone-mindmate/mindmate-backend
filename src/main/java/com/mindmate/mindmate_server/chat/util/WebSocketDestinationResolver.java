package com.mindmate.mindmate_server.chat.util;

import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import org.springframework.stereotype.Component;

@Component
public class WebSocketDestinationResolver {
    public String getDestinationByEventType(String roomId, String eventType) {
        try {
            ChatEventType type = ChatEventType.valueOf(eventType);

            switch (type) {
                case MESSAGE:
                    return "/topic/chat.room." + roomId;
                case READ_STATUS:
                    return "/topic/chat.room." + roomId + ".read";
                case REACTION:
                    return "/topic/chat.room." + roomId + ".reaction";
                case CUSTOM_FORM:
                case CUSTOM_FORM_RESPONSE:
                    return "/topic/chat.room." + roomId + ".customform";
                case EMOTICON:
                    return "/topic/chat.room." + roomId + ".emoticon";  // 추가
                default:
                    return "/topic/chat.room." + roomId;
            }
        } catch (IllegalArgumentException e) {
            return "/topic/chat.room." + roomId;
        }
    }
}
