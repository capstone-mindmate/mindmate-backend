package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String profileImage;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Integer entranceTime;

    @Column(nullable = false)
    private boolean graduation;

    // 활동 정보
    private int counselingCount = 0;
    private int avgResponseTime = 0;
    private int totalResponseTime = 0;
    private int responseTimeCount = 0;

    // 평가 태그
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationTag> evaluationTags = new ArrayList<>();


    @Builder
    public Profile(User user, String nickname, String department, Integer entranceTime, boolean graduation, String profileImage) {
        this.user = user;
        this.profileImage = profileImage;
        this.nickname = nickname;
        this.department = department;
        this.entranceTime = entranceTime;
        this.graduation = graduation;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void incrementCounselingCount() {
        this.counselingCount++;
    }

    public void updateResponseTime(int newResponseTime) {
        this.totalResponseTime += newResponseTime;
        this.responseTimeCount++;
        this.avgResponseTime = this.responseTimeCount > 0
                ? this.totalResponseTime / this.responseTimeCount
                : 0;
    }

    public void addEvaluationTag(EvaluationTag tag) {
        this.evaluationTags.add(tag);
    }

    public void updateDepartment(String department) {
        this.department = department;
    }

    public void updateEntranceTime(Integer entranceTime) {
        this.entranceTime = entranceTime;
    }

    public void updateGraduation(Boolean graduation) {
        this.graduation = graduation;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}