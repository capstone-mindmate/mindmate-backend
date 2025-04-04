package com.mindmate.mindmate_server.magazine.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "magazine_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"magazine_id", "userId"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MagazineLike extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public MagazineLike(Magazine magazine, User user) {
        this.magazine = magazine;
        this.user = user;
    }
}
