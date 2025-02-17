package com.mindmate.mindmate_server.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "listener_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ListenerProfile {
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
    private CounselingStyle counseling_style;

    private LocalDateTime available_time;
    private final Integer counseling_count = 0;
    private final Integer avg_response_time = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public ListenerProfile(User user, String nickname) {
        this.user = user;
        this.nickname = nickname;
    }
}
