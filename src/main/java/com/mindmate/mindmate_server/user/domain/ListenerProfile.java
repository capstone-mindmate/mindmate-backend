package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounselingStyle counselingStyle;

    private LocalDateTime availableTime;
    private Integer counselingCount = 0;
    private Integer avgResponseTime = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

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
}
