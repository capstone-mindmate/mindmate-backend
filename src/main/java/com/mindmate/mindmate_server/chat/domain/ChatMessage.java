package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType senderRole;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String filteredContent;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private boolean isRead;
    private LocalDateTime readAt;
//    private LocalDateTime expiryTime;

    @Builder
    public ChatMessage(ChatRoom chatRoom, User sender, RoleType senderRole, String content, MessageType type) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.senderRole = senderRole;
        this.content = content;
        this.type = type;
        this.isRead = false;

        // todo : 메시지 생성 시 상대방의 읽은 않은 메시지 수 증가? -> 온라인/오프라인 여부 확인해야함
    }

    public void setFilteredContent(String filteredContent) {
        this.filteredContent = filteredContent;
    }
}
