package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomResponse {
    private Long roomId;
    private ChatRoomStatus chatRoomStatus;
    private int unreadCount;

    private LocalDateTime lastMessageTime;
    private String lastMessage;

    private String oppositeName;
    private String oppositeImage;
    private String userRole;

    // todo: 채팅방 or 매칭방 이름 + 자신의 역할
    public static ChatRoomResponse from(ChatRoom chatRoom, User user) {
        boolean isListener = chatRoom.isListener(user);

        User opposite = isListener ? chatRoom.getSpeaker() : chatRoom.getListener();
        String oppositeName = opposite.getProfile().getNickname();
        String oppositeImage = opposite.getProfile().getProfileImage();

        int unreadCount = isListener ? chatRoom.getListenerUnreadCount() : chatRoom.getSpeakerUnreadCount();
        String userRole = isListener ? "LISTENER" : "SPEAKER";

        String lastMessage = null;
        if (!chatRoom.getMessages().isEmpty()) {
            lastMessage = chatRoom.getMessages().get(chatRoom.getMessages().size() - 1).getContent();
        }

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .chatRoomStatus(chatRoom.getChatRoomStatus())
                .unreadCount(unreadCount)
                .lastMessageTime(chatRoom.getLastMessageTime())
                .lastMessage(lastMessage)
                .oppositeName(oppositeName)
                .oppositeImage(oppositeImage)
                .userRole(userRole)
                .build();
    }
}
