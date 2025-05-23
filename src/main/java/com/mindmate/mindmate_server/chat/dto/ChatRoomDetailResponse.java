package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ChatRoomDetailResponse {
    private Long roomId;
    private Long matchingId;
    private LocalDateTime createdAt;
    private ChatRoomStatus roomStatus;
    private InitiatorType closeRequestRole;
    private LocalDateTime closeRequestedAt;
    private boolean isListener;

    // todo: 상대방 아이디/이미지 + 내 이미지/아이디 + 방 생성자?(수정 권한)
    private List<ChatMessageResponse> messages;
    private String myImageUrl;
    private String oppositeImageUrl;

    public static ChatRoomDetailResponse from(ChatRoom chatRoom, List<ChatMessage> messages, User user) {
        boolean isListener = chatRoom.isListener(user);

        return ChatRoomDetailResponse.builder()
                .roomId(chatRoom.getId())
                .matchingId(chatRoom.getMatching() != null ? chatRoom.getMatching().getId() : null)
                .createdAt(chatRoom.getCreatedAt())
                .roomStatus(chatRoom.getChatRoomStatus())
                .closeRequestRole(chatRoom.getClosureRequesterRole())
                .closeRequestedAt(chatRoom.getClosureRequestAt())
                .isListener(isListener)
                .messages(messages.stream()
                        .map(message -> ChatMessageResponse.from(message, user.getId()))
                        .collect(Collectors.toList()))
                .myImageUrl(isListener ? chatRoom.getListener().getProfile().getProfileImage().getImageUrl() : chatRoom.getSpeaker().getProfile().getProfileImage().getImageUrl())
                .oppositeImageUrl(isListener ? chatRoom.getSpeaker().getProfile().getProfileImage().getImageUrl() : chatRoom.getListener().getProfile().getProfileImage().getImageUrl())
                .build();
    }
}
