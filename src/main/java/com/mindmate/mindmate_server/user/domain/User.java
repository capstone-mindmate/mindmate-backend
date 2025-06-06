package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.CustomForm;
import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineLike;
import com.mindmate.mindmate_server.matching.domain.Matching;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private RoleType currentRole;

    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column
    private LocalDateTime lastLoginAt;


    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<ChatMessage> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "creator")
    private List<Matching> createdMatchings = new ArrayList<>();

    @OneToMany(mappedBy = "acceptedUser")
    private List<Matching> acceptedMatchings = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Profile profile;

    @OneToMany(mappedBy = "user")
    private List<MessageReaction> messageReactions = new ArrayList<>();

    @OneToMany(mappedBy = "creator")
    private List<CustomForm> createdCustomForms = new ArrayList<>();

    @OneToMany(mappedBy = "responder")
    private List<CustomForm> respondedCustomForms = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<Magazine> magazines = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<MagazineLike> magazineLikes = new ArrayList<>();

    private int reportCount = 0;
    private LocalDateTime suspensionEndTime;

    @Builder
    public User(String email, AuthProvider provider, String providerId, RoleType role) {
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.currentRole = role;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateRole(RoleType type) {
        log.info("Updating role from {} to {}", this.currentRole, type);
        this.currentRole = type;
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public void suspend(Duration duration) {
        this.currentRole = RoleType.ROLE_SUSPENDED;
        this.suspensionEndTime = LocalDateTime.now().plus(duration);
    }

    public void unsuspend() {
        this.currentRole = RoleType.ROLE_PROFILE;
        this.suspensionEndTime = null;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

}
