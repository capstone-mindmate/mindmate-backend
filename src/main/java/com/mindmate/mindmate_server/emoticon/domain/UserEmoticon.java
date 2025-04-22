package com.mindmate.mindmate_server.emoticon.domain;

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
public class UserEmoticon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emoticon_id", nullable = false)
    private Emoticon emoticon;

    @Column(nullable = false)
    private boolean isPurchased;

    @Column
    private int purchasePrice;

    @Builder
    public UserEmoticon(User user, Emoticon emoticon, boolean isPurchased, int purchasePrice) {
        this.user = user;
        this.emoticon = emoticon;
        this.isPurchased = isPurchased;
        this.purchasePrice = purchasePrice;
    }
}
