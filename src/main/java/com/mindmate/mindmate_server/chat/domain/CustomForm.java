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
@Table(name = "custom_forms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomForm extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id")
    private User responder;

    @Column(nullable = true)
    private boolean isAnswered = false;

    @Column(nullable = true)
    private LocalDateTime answeredAt;

    @OneToMany(mappedBy = "customForm", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomFormItem> items = new ArrayList<>();

    @Builder
    public CustomForm(ChatRoom chatRoom, User creator, User responder) {
        this.chatRoom = chatRoom;
        this.creator = creator;
        this.responder = responder;
    }

    public void markAsAnswered() {
        this.isAnswered = true;
        this.answeredAt = LocalDateTime.now();
    }

    public void addItem(String question) {
        CustomFormItem item = CustomFormItem.builder()
                .customForm(this)
                .question(question)
                .build();
        this.items.add(item);
    }

}
