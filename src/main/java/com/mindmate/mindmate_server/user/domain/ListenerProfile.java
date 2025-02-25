package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
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
@Table(name = "listener_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListenerProfile extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String profileImage;

    @OneToMany(mappedBy = "listenerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ListenerCounselingField> counselingFields = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounselingStyle counselingStyle;

    private LocalDateTime availableTime;
    private Integer counselingCount = 0;
    private Integer avgResponseTime = 0;
    private Float averageRates = 0.0f;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "listener", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoom> chatRooms = new ArrayList<>();

    private String certificationUrl;

    @Column(columnDefinition = "TEXT")
    private String careerDescription;

    @Enumerated(EnumType.STRING)
    private Badge badgeStatus;


    @Builder
    public ListenerProfile(User user, String nickname, String profileImage, CounselingStyle counselingStyle, LocalDateTime availableTime) {
        this.user = user;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.counselingStyle = counselingStyle;
        this.availableTime = availableTime;
    }

    public void addCounselingField(CounselingField field) {
        ListenerCounselingField counselingField = ListenerCounselingField.builder()
                .listenerProfile(this)
                .field(field)
                .build();
        this.counselingFields.add(counselingField);
    }

    public void removeCounselingField(CounselingField field) {
        this.counselingFields.removeIf(cf -> cf.getField() == field);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateCounselingStyle(CounselingStyle counselingStyle) {
        this.counselingStyle = counselingStyle;
    }

    public void updateCounselingFields(List<CounselingField> fields) {
        this.counselingFields.clear();
        fields.forEach(this::addCounselingField);
    }

    public void incrementCounselingCount() {
        this.counselingCount++;
    }

    public void updateAverageResponseTime(Integer newResponseTime) {
        if (this.avgResponseTime == 0) {
            this.avgResponseTime = newResponseTime;
        } else {
            double weight = 0.2; // 가중치 임의로 설정함
            this.avgResponseTime = (int) ((1 - weight) * this.avgResponseTime + weight * newResponseTime);
        }
    }

    public void updateAverageRating(Float newRating, Long totalReviews) {
        if (totalReviews == 1) {
            this.averageRates = newRating;
        } else {
            this.averageRates = ((this.averageRates * (totalReviews - 1)) + newRating) / totalReviews;
        }
    }

    public void approveCertification() {
        this.badgeStatus = Badge.EXPERT;
        this.certificationUrl = null; // 자료 삭제
    }

    /* 특정 조건 만족 시 배지 변경할 수 있도록 하는 로직 */

    public void rejectCertification() {
        this.certificationUrl = null;
    }

    public void updateCertificationDetails(String certificationUrl, String careerDescription) {
        this.certificationUrl = certificationUrl;
        this.careerDescription = careerDescription;
    }

    public void updateAvailableTime(LocalDateTime availableTimes) {
        this.availableTime = availableTimes;
    }
}
