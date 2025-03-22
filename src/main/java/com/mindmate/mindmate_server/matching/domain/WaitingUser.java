package com.mindmate.mindmate_server.matching.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "waiting_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matching_id", nullable = false)
    private Matching matching;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User waitingUser;

    @Column(length = 100)
    private String message;

    @Enumerated(EnumType.STRING)
    private MatchingType matchingType = MatchingType.MANUAL;

    @Enumerated(EnumType.STRING)
    private WaitingStatus status = WaitingStatus.PENDING;

    private boolean isAnonymous; // 프로필 보여줄지 말지

    @Builder
    public WaitingUser(User waitingUser, String message, MatchingType matchingType, boolean isAnonymous) {
        this.waitingUser = waitingUser;
        this.message = message;
        this.matchingType = matchingType;
        this.isAnonymous = isAnonymous;
    }

    public void setMatching(Matching matching) {
        this.matching = matching;
    }

    public void accept() {
        this.status = WaitingStatus.ACCEPTED;
    }

    public void reject() {
        this.status = WaitingStatus.REJECTED;
    }

    public boolean isOwner(User user) {
        return this.waitingUser.getId().equals(user.getId());
    }

    public boolean isAutoMatching() {
        return this.matchingType == MatchingType.AUTO_FORMAT;
    }

}
