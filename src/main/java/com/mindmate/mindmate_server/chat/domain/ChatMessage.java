package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "message")
    private List<MessageReaction> messageReactions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_form_id")
    private CustomForm customForm;

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private RoleType senderRole;

    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Transient
    private String decryptedContent;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private boolean isRead;
    private LocalDateTime readAt;

    private boolean encrypted = false;
    
    @Builder
    public ChatMessage(ChatRoom chatRoom, User sender, String content, MessageType type) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.isRead = false;
    }

    public boolean isCustomForm() {
        return this.type == MessageType.CUSTOM_FORM;
    }

    public void setCustomForm(CustomForm customForm) {
        this.customForm = customForm;
    }

    public void updateEncryptedContent(String encryptedContent) {
        this.content = encryptedContent;
        this.encrypted = true;
    }
    
    public void setDecryptedContent(String decryptedContent) {
        this.decryptedContent = decryptedContent;
    }
    
    // 내용 반환 시에 복호화된 내용을 우선적으로 사용
    public String getContent() {
        if (decryptedContent != null) {
            return decryptedContent;
        }
        return content;
    }
}
