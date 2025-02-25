package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "speaker_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpeakerProfile extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nickname;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounselingStyle preferredCounselingStyle;

    private Integer counselingCount = 0;
    private Float averageRates = 0.0f;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "speaker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoom> chatRooms = new ArrayList<>();

    @Builder
    public SpeakerProfile(User user, String nickname, String profileImage, CounselingStyle preferredCounselingStyle) {
        this.user = user;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.preferredCounselingStyle = preferredCounselingStyle;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateCounselingStyle(CounselingStyle counselingStyle) {
        this.preferredCounselingStyle = counselingStyle;
    }

    public void incrementCounselingCount() {
        this.counselingCount++;
    }

    public void updateAverageRating(Float newRating, Long totalReviews) {
        if (totalReviews == 1) {
            this.averageRates = newRating;
        } else {
            this.averageRates = ((this.averageRates * (totalReviews - 1)) + newRating) / totalReviews;
        }
    }

}
