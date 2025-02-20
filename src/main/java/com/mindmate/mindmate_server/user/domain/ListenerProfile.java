package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "listener_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListenerProfile extends BaseTimeEntity {
    /**
     * 향후 추가 사항
     * 1. 경력 인증 관련 필드
     * 2. available_time 관리 어떻게 할지
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String profileImage;

    @OneToMany(mappedBy = "listenerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ListenerCounselingField> counselingFields = new HashSet<>();

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private CounselingStyle counselingStyle;

    @Column(columnDefinition = "JSON")
    private String availableTimes;

    private Integer counselingCount = 0;
    private Integer avgResponseTime = 0; // 분이나 시간
    private Float averageRates = 0.0f;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "badge_status")
    private String badgeStatus;

    @Builder
    public ListenerProfile(User user, String nickname, CounselingStyle counselingStyle) {
        this.user = user;
        this.nickname = nickname;
        this.counselingStyle = counselingStyle;
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

    public void addAvailableTimes(String timeJson) {
        if (this.availableTimes == null) {
            this.availableTimes = "[]";
        }
        // 클라이언트에서 json으로 데이터 받아야 할듯
        this.availableTimes = timeJson;
    }

    public void updateAvailableTime(String availableTimeJson) {
        this.availableTimes = availableTimeJson;
    }

//    public void clearAvailableTime() {
//        this.availableTimes = "[]";
//    } // 한 번에 지워버릴때?

    public void updateCounselingFields(List<CounselingField> fields) {
        this.counselingFields.clear();
        fields.forEach(this::addCounselingField);
    }

    public void updateBadgeStatus(String badgeStatus) {
        this.badgeStatus = badgeStatus;
    }

    public void incrementCounselingCount() {
        this.counselingCount++;
    }

    public void updateAverageResponseTime(Integer newResponseTime) {
        this.avgResponseTime = newResponseTime;
    }

    public void updateAverageRating(Float newRating) {

        this.averageRates = newRating;
    } // 계산하는 로직 작성하기

}
