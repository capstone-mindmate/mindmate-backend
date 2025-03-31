package com.mindmate.mindmate_server.review.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.Profile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evaluations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationTag extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @Column(nullable = false)
    private String tagContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType tagType;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private Long evaluatorId;

    @Builder
    public EvaluationTag(Profile profile, String tagContent, TagType tagType,
                         Long chatRoomId, Long evaluatorId) {
        this.profile = profile;
        this.tagContent = tagContent;
        this.tagType = tagType;
        this.chatRoomId = chatRoomId;
        this.evaluatorId = evaluatorId;
    }
}