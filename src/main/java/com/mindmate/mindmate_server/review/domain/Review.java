package com.mindmate.mindmate_server.review.domain;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
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
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_profile_id", nullable = false)
    private Profile reviewedProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private int rating;

    @Column(length = 200)
    private String comment;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationTag> reviewTags = new ArrayList<>();

    @Column(nullable = false)
    private boolean anonymous;

    @Builder
    public Review(ChatRoom chatRoom, User reviewer, Profile reviewedProfile, int rating, String comment, boolean anonymous) {
        this.chatRoom = chatRoom;
        this.reviewer = reviewer;
        this.reviewedProfile = reviewedProfile;
        this.rating = rating;
        this.comment = comment;
        this.anonymous = anonymous;
    }

    public void addTag(Tag tag) {
        EvaluationTag reviewTag = EvaluationTag.builder()
                .review(this)
                .tagContent(tag)
                .build();
        this.reviewTags.add(reviewTag);
    }
}
