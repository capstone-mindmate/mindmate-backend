package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Slf4j
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"sentMessages"})
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;



    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    private boolean isEmailVerified = false;
    private boolean agreedToTerms = false;

    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private RoleType currentRole;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<ChatMessage> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "listener")
    private List<ChatRoom> listenerRooms = new ArrayList<>();

    @OneToMany(mappedBy = "speaker")
    private List<ChatRoom> speakerRooms = new ArrayList<>();

    @Builder
    public User(String email, String password, RoleType role) {
        this.email = email;
        this.password = password;
        this.currentRole = role;
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
        log.info("Updating role from {} to {}", this.currentRole, type);
        this.currentRole = type;
    }

    public void generateVerificationToken() {
        this.verificationToken = UUID.randomUUID().toString();
        this.verificationTokenExpiry = LocalDateTime.now().plusHours(24);
    }

    public boolean isTokenExpired() {
        return this.getVerificationTokenExpiry().isBefore(LocalDateTime.now());
    }


}
