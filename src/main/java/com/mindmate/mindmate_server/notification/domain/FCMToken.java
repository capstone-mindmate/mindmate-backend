package com.mindmate.mindmate_server.notification.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FCMToken extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    private boolean active = true;

    @Builder
    public FCMToken(User user, String token, String deviceInfo) {
        this.user = user;
        this.token = token;
    }
}