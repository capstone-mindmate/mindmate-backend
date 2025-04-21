package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.CustomForm;
import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineLike;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private RoleType currentRole;


    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    private boolean isEmailVerified = false;
    private boolean agreedToTerms = false;

    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;

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


    // 일일 매칭 제한 관련 필드 추가
    private int dailyCancellationCount = 0;
    private int dailyRejectionCount = 0;
    private LocalDate lastActionResetDate;

    // 신고 관련 데이터 처리
    private int reportCount = 0;
    private LocalDateTime suspensionEndTime;

    @Builder
    public User(String email, String password, boolean agreedToTerms, RoleType role) {
        this.email = email;
        this.password = password;
        this.agreedToTerms = agreedToTerms;
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


    // 매칭 관련 메서드 추가
    public boolean addCancelCount() {
        checkAndResetLimits();

        if (dailyCancellationCount >= 3) {
            return false; // 한도 초과
        }

        dailyCancellationCount++;
        return true;
    }

    public boolean addRejectionCount() {
        checkAndResetLimits();

        if (dailyRejectionCount >= 3) {
            return false; // 한도 초과
        }

        dailyRejectionCount++;
        return true;
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

    private void checkAndResetLimits() {
        LocalDate today = LocalDate.now();
        if (lastActionResetDate == null || !today.equals(lastActionResetDate)) {
            dailyCancellationCount = 0;
            dailyRejectionCount = 0;
            lastActionResetDate = today;
        }
    }
}
