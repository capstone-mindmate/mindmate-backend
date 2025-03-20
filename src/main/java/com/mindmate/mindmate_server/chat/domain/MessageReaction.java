package com.mindmate.mindmate_server.chat.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message_reactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageReaction extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    @Builder
    public MessageReaction(ChatMessage message, User user, ReactionType reactionType) {
        this.message = message;
        this.user = user;
        this.reactionType = reactionType;
    }

    public void updateReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }
}
