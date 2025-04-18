package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id")
    private Matching matching;

    @Enumerated(EnumType.STRING)
    private ChatRoomStatus chatRoomStatus;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomForm> customForms = new ArrayList<>();

    private LocalDateTime lastMessageTime;
    private LocalDateTime closedAt;

    private Long listenerLastReadMessageId = 0L;
    private Long speakerLastReadMessageId = 0L;
    private Long listenerUnreadCount = 0L;
    private Long speakerUnreadCount = 0L;

    @Enumerated(EnumType.STRING)
    private InitiatorType closureRequesterRole;
    private LocalDateTime closureRequestAt;

    @Builder
    public ChatRoom(Matching matching) {
        this.matching = matching;
        this.chatRoomStatus = ChatRoomStatus.PENDING;
    }

    public void updateLastMessageTime() {
        this.lastMessageTime = LocalDateTime.now();
    }

    public void close() {
        this.chatRoomStatus = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void markAsReadForListener(Long messageId) {
        this.listenerLastReadMessageId = messageId;
        this.listenerUnreadCount = 0L;
    }

    public void markAsReadForSpeaker(Long messageId) {
        this.speakerLastReadMessageId = messageId;
        this.speakerUnreadCount = 0L;
    }

    public void increaseUnreadCountForListener() {
        this.listenerUnreadCount++;
        log.info("Current listenerUnreadCOunt: {}", listenerUnreadCount);
    }

    public void increaseUnreadCountForSpeaker() {
        this.speakerUnreadCount++;
        log.info("Current speakerUnreadCount: {}", speakerUnreadCount);
    }


    public User getListener() {
        if (matching.getCreatorRole() == InitiatorType.LISTENER) {
            return matching.getCreator();
        } else {
            return matching.getAcceptedUser();
        }
    }

    public User getSpeaker() {
        if (matching.getCreatorRole() == InitiatorType.SPEAKER) {
            return matching.getCreator();
        } else {
            return matching.getAcceptedUser();
        }
    }

    public boolean isListener(User user) {
        return getListener().equals(user);
    }

    public boolean isSpeaker(User user) {
        return getSpeaker().equals(user);
    }

    public void markAsRead(User user, Long messageId) {
        if (isListener(user)) {
            markAsReadForListener(messageId);
        } else if (isSpeaker(user)) {
            markAsReadForSpeaker(messageId);
        }
    }

    public void increaseUnreadCount(User sender) {
        if (isSpeaker(sender)) {
            increaseUnreadCountForListener();
        } else if (isListener(sender)) {
            increaseUnreadCountForSpeaker();
        }
    }

    public void updateChatRoomStatus(ChatRoomStatus status) {
        this.chatRoomStatus = status;
    }

    public void requestClosure(User user) {
        this.chatRoomStatus = ChatRoomStatus.CLOSE_REQUEST;
        this.closureRequesterRole = isListener(user) ? InitiatorType.LISTENER : InitiatorType.SPEAKER;
        this.closureRequestAt = LocalDateTime.now();
    }

    public void rejectClosure() {
        this.chatRoomStatus = ChatRoomStatus.ACTIVE;
        this.closureRequesterRole = null;
        this.closureRequestAt = null;
    }

    public boolean isClosureRequester(User user) {
        if (closureRequesterRole == null) return false;

        if (closureRequesterRole == InitiatorType.LISTENER) {
            return isListener(user);
        } else {
            return isSpeaker(user);
        }
    }

    public void acceptClosure() {
        this.chatRoomStatus = ChatRoomStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

}
