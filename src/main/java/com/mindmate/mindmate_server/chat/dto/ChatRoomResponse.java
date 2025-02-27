package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.common.value.qual.ArrayLen;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponse {
    private Long roomId;
    private ChatRoomStatus chatRoomStatus;
    private int unreadCount;
    private LocalDateTime lastMessageTime;
    private Long lastReadMessageId;

    private String myName;
    private String oppositeName;

    public static ChatRoomResponse from(ChatRoom chatRoom, Long userId) {
        boolean isListener = chatRoom.getListener().getUser().getId().equals(userId);
        int unreadCount = isListener ? chatRoom.getListenerUnreadCount() : chatRoom.getSpeakerUnreadCount();
        Long lastReadMessageId = isListener ? chatRoom.getListenerLastReadMessageId() : chatRoom.getSpeakerLastReadMessageId();
        String myName = isListener ? chatRoom.getListener().getNickname() : chatRoom.getSpeaker().getNickname();
        String oppositeName = isListener ? chatRoom.getSpeaker().getNickname() : chatRoom.getListener().getNickname();

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .chatRoomStatus(chatRoom.getChatRoomStatus())
                .unreadCount(unreadCount)
                .lastMessageTime(chatRoom.getLastMessageTime())
                .lastReadMessageId(lastReadMessageId)
                .myName(myName)
                .oppositeName(oppositeName)
                .build();
    }

}
