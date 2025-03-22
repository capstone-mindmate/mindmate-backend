package com.mindmate.mindmate_server.matching.domain;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
//import com.mindmate.mindmate_server.user.domain.CounselingField;
//import com.mindmate.mindmate_server.user.domain.ListenerProfile;
//import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "matchings")
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

    @ElementCollection
    @CollectionTable(
            name = "matching_categories",
            joinColumns = @JoinColumn(name = "matching_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Set<MatchingCategory> categories = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private MatchingStatus status;

    @Enumerated(EnumType.STRING)
    private InitiatorType creatorRole; // 요청 주체

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private User acceptedUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    private LocalDateTime matchedAt;

    private boolean anonymous;
    private boolean allowRandom;
    private boolean showDepartment;

    @OneToMany(mappedBy = "matching", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WaitingUser> waitingUsers = new ArrayList<>();

    @Builder
    public Matching(User creator, String title, String description, Set<MatchingCategory> categories, InitiatorType creatorRole, boolean anonymous, boolean allowRandom, boolean showDepartment) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.creatorRole = creatorRole;
        this.anonymous = anonymous;
        this.allowRandom = allowRandom;
        this.showDepartment = showDepartment;

        if (categories != null && !categories.isEmpty()) {
            this.categories.addAll(categories);
        }

        this.status = MatchingStatus.OPEN;
    }

    public void setChatRoom(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }

    public void closeMatching() {
        this.status = MatchingStatus.CLOSED;
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

    public boolean isCreatorSpeaker() {
        return this.creatorRole == InitiatorType.SPEAKER;
    }

    public InitiatorType getRequiredRole() {
        return this.creatorRole == InitiatorType.SPEAKER
                ? InitiatorType.LISTENER
                : InitiatorType.SPEAKER;
    }

    public int getWaitingUsersCount() {
        return this.waitingUsers.size();
    }

    public void updateMatchingInfo(String title, String description, Set<MatchingCategory> categories,
                                   boolean anonymous, boolean allowRandom, boolean showDepartment) {

        this.title = title;
        this.description = description;

        this.categories.clear();
        if (categories != null && !categories.isEmpty()) {
            this.categories.addAll(categories);
        }

        this.anonymous = anonymous;
        this.allowRandom = allowRandom;
        this.showDepartment = showDepartment;
    }
}
