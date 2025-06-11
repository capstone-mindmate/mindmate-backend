package com.mindmate.mindmate_server.matching.domain;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matchings", indexes = {
        @Index(name = "idx_matching_status_category", columnList = "status,category"),
        @Index(name = "idx_matching_status_creatorRole", columnList = "status,creator_role")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Matching extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String description;

    @Enumerated(EnumType.STRING)
    private MatchingCategory category;

    @Enumerated(EnumType.STRING)
    private MatchingStatus status;

    @Enumerated(EnumType.STRING)
    private InitiatorType creatorRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private User acceptedUser;

    @OneToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    private LocalDateTime matchedAt;

    private boolean anonymous;
    private boolean allowRandom;
    private boolean showDepartment;

    @OneToMany(mappedBy = "matching", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WaitingUser> waitingUsers = new ArrayList<>();

    @Builder
    public Matching(User creator, String title, String description, MatchingCategory category, InitiatorType creatorRole, boolean anonymous, boolean allowRandom, boolean showDepartment) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.creatorRole = creatorRole;
        this.anonymous = anonymous;
        this.allowRandom = allowRandom;
        this.showDepartment = showDepartment;
        this.category = category;

        this.status = MatchingStatus.OPEN;
    }

    public void setChatRoom(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }

    public void cancelMatching() {
        this.status = MatchingStatus.CANCELED;
        this.chatRoom = null;
    }

    public void rejectMatching() {
        this.status = MatchingStatus.REJECTED;
        this.chatRoom = null;
    }

    public void acceptMatching(User acceptedUser) {
        this.acceptedUser = acceptedUser;
        this.matchedAt = LocalDateTime.now();
        this.status = MatchingStatus.MATCHED;
    }

    public boolean isOpen() {
        return this.status == MatchingStatus.OPEN;
    }

    public void addWaitingUser(WaitingUser waitingUser) {
        this.waitingUsers.add(waitingUser);
        waitingUser.setMatching(this);
    }

    public boolean isCreator(User user) {
        return this.creator.getId().equals(user.getId());
    }

    public int getWaitingUsersCount() {
        return this.waitingUsers.size();
    }

    public void updateMatchingInfo(String title, String description, MatchingCategory category,
                                   boolean anonymous, boolean allowRandom, boolean showDepartment) {

        this.title = title;
        this.description = description;
        this.category = category;
        this.anonymous = anonymous;
        this.allowRandom = allowRandom;
        this.showDepartment = showDepartment;
    }
}
