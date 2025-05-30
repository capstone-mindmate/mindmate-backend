package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id", nullable = true,
            foreignKey = @ForeignKey(
                    foreignKeyDefinition = "FOREIGN KEY (profile_image_id) REFERENCES profile_images(id) ON DELETE SET NULL"))
    private ProfileImage profileImage;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Integer entranceTime;

    @Column(nullable = false)
    private boolean graduation;

    @Column(nullable = false)
    private boolean agreedToTerms = false;


    private int counselingCount = 0;
    private int avgResponseTime = 0;
    private int totalResponseTime = 0;
    private int responseTimeCount = 0;
    private double ratingSum = 0;

    @Version
    private Long version;

    @Builder
    public Profile(User user, String nickname, String department, Integer entranceTime, boolean graduation, ProfileImage profileImage, boolean agreedToTerms) {
        this.user = user;
        this.profileImage = profileImage;
        this.nickname = nickname;
        this.department = department;
        this.entranceTime = entranceTime;
        this.graduation = graduation;
        this.agreedToTerms = agreedToTerms;
    }

    public void updateProfileImage(ProfileImage profileImage) {this.profileImage = profileImage;}
    public void incrementCounselingCount() {
        this.counselingCount++;
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

    public double getAvgRating() {
        return this.counselingCount > 0
                ? this.ratingSum / this.counselingCount
                : 0;
    }
    public void updateRating(double rating){
        this.ratingSum += rating;
    }

    public void decrementCounselingCount() {
        if (this.counselingCount > 0) {
            this.counselingCount--;
        }
    }

    public void subtractRating(double rating) {
        this.ratingSum -= rating;
        if (this.ratingSum < 0) {
            this.ratingSum = 0;
        }
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