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

    @Enumerated(EnumType.STRING)
    private MatchingType type;

    @Enumerated(EnumType.STRING)
    private MatchingStatus status;

    @Enumerated(EnumType.STRING)
    private InitiatorType creatorRole; // 요청 주체

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private User acceptedUser;

    @OneToOne(fetch = FetchType.LAZY)
    @Column(name = "chat_room_id")
    private ChatRoom chatRoom;

    private LocalDateTime matchedAt;

    @OneToMany(mappedBy = "matchingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WaitingUser> waitingUsers = new ArrayList<>();

    @Builder
    public Matching(User creator, String title, String description, InitiatorType creatorRole) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.creatorRole = creatorRole;
        this.status = MatchingStatus.REQUESTED;
    }
}
