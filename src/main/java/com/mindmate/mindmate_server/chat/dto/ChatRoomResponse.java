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
    private Long matchingId;

    private ChatRoomStatus chatRoomStatus;
    private Long unreadCount;
    private LocalDateTime lastMessageTime;
    private String lastMessage;

    private Long oppositeId;
    private String oppositeName;
    private String oppositeImage;
    private String userRole;

    private boolean isCreator;
    private String matchingTitle;


    // todo: 매칭방 타입
    public static ChatRoomResponse from(ChatRoom chatRoom, User user) {
        boolean isListener = chatRoom.isListener(user);

        User opposite = isListener ? chatRoom.getSpeaker() : chatRoom.getListener();
        String oppositeName = opposite.getProfile().getNickname();
        String oppositeImage = opposite.getProfile().getProfileImage();

        Long unreadCount = isListener ? chatRoom.getListenerUnreadCount() : chatRoom.getSpeakerUnreadCount();
        String userRole = isListener ? "LISTENER" : "SPEAKER";

        boolean isCreator = chatRoom.getMatching().getCreator().equals(user);

        String lastMessage = null;
        if (!chatRoom.getMessages().isEmpty()) {
            lastMessage = chatRoom.getMessages().get(chatRoom.getMessages().size() - 1).getContent();
        }

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .matchingId(chatRoom.getMatching().getId())
                .chatRoomStatus(chatRoom.getChatRoomStatus())
                .unreadCount(unreadCount)
                .lastMessageTime(chatRoom.getLastMessageTime())
                .lastMessage(lastMessage)
                .oppositeId(opposite.getId())
                .oppositeName(oppositeName)
                .oppositeImage(oppositeImage)
                .userRole(userRole)
                .isCreator(isCreator)
                .matchingTitle(chatRoom.getMatching().getTitle())
                .build();
    }
}
