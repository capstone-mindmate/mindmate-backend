package com.mindmate.mindmate_server.emoticon.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_emoticons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEmoticon extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emoticon_id", nullable = false)
    private Emoticon emoticon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmoticonType type;

    @Column
    private int purchasePrice;

    @Builder
    public UserEmoticon(User user, Emoticon emoticon, EmoticonType type, int purchasePrice) {
        this.user = user;
        this.emoticon = emoticon;
        this.type = type;
        this.purchasePrice = purchasePrice;
    }
}
