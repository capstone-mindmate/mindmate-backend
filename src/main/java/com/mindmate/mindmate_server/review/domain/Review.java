package com.mindmate.mindmate_server.review.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    private RoleType reviewerRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id")
    private User reviewee;

    @Enumerated(EnumType.STRING)
    private RoleType revieweeRole;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "matching_id")
//    private Matching matching;

    private double rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    // tag를 어떤식으로 제공하지?
//    private List<String> tags = new ArrayList<>();

    private String reply;

    @Builder
    public Review(User reviewer, User reviewee, double rating,
                  String content ) {
        this.reviewer = reviewer;
        this.reviewee = reviewee;
//        this.matching = matching;
        this.rating = rating;
        this.content = content;
//        this.tags = tags;
    }

    public void addReply(String reply) {
        this.reply = reply;
    }
}
