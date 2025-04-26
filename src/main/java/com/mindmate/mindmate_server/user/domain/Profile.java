package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
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

    private int counselingCount = 0;
    private int avgResponseTime = 0;
    private int totalResponseTime = 0;
    private int responseTimeCount = 0;

    private double avgRating = 0;
    private double ratingSum = 0;

    @Version
    private Long version;

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

    public void updateAvgRating(double rating){
        this.ratingSum += rating;
        this.avgRating = this.counselingCount > 0 ? this.ratingSum / this.counselingCount : 0;
    }

    public void addMultipleResponseTimes(List<Integer> responseTimes) {
        if (responseTimes.isEmpty()) {
            return;
        }

        int sum = responseTimes.stream().mapToInt(Integer::intValue).sum();
        this.totalResponseTime += sum;
        this.responseTimeCount += responseTimes.size();
        this.avgResponseTime = this.responseTimeCount > 0
                ? this.totalResponseTime / this.responseTimeCount
                : 0;
    }
}