package com.mindmate.mindmate_server.user.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private final Integer counselingCount = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public SpeakerProfile(User user, String nickname) {
        this.user = user;
        this.nickname = nickname;
    }
}
