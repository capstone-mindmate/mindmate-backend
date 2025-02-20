package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"listenerProfile", "speakerProfile"})
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    private boolean isEmailVerified = false;
    private boolean agreedToTerms = false;

    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType currentRole;

    @OneToOne(mappedBy = "user")
    private ListenerProfile listenerProfile;

    @OneToOne(mappedBy = "user")
    private SpeakerProfile speakerProfile;

    @Builder
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void verifyEmail() {
        this.isEmailVerified = true;
    }

    public void checkAgreement() {
        this.agreedToTerms = true;
    }

    public void updateRole(RoleType type) {
        this.currentRole = type;
    }

}
