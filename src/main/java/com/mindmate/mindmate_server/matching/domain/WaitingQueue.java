package com.mindmate.mindmate_server.matching.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "waiting_queue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingQueue extends BaseTimeEntity { // 대기상태 관리
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_profile_id")
    private SpeakerProfile speakerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listener_profile_id")
    private ListenerProfile listenerProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InitiatorType waitingType;

    @ElementCollection
    @CollectionTable(name = "waiting_queue_preferred_fields",
            joinColumns = @JoinColumn(name = "waiting_queue_id"))
    @Enumerated(EnumType.STRING)
    private Set<CounselingField> preferredFields = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private CounselingStyle preferredStyle;

    private boolean active = true; // 대기중일 때 true, 완료 or 취소일 때 false

    @Builder
    public WaitingQueue(SpeakerProfile speakerProfile, ListenerProfile listenerProfile,
                        InitiatorType waitingType, Set<CounselingField> preferredFields,
                        CounselingStyle preferredStyle) {
        if ((speakerProfile != null && listenerProfile != null) ||
                (speakerProfile == null && listenerProfile == null)) {
            throw new CustomException(MatchingErrorCode.INVALID_WAITING_QUEUE);
        } // 하나만 들어와야함

        if (speakerProfile != null && waitingType != InitiatorType.SPEAKER) {
            throw new CustomException(MatchingErrorCode.INVALID_WAITING_QUEUE);
        }

        if (listenerProfile != null && waitingType != InitiatorType.LISTENER) {
            throw new CustomException(MatchingErrorCode.INVALID_WAITING_QUEUE);
        }

        this.speakerProfile = speakerProfile;
        this.listenerProfile = listenerProfile;
        this.waitingType = waitingType;
        this.preferredFields = preferredFields != null ? preferredFields : new HashSet<>();
        this.preferredStyle = preferredStyle;
    }

    public void deactivate() {
        this.active = false;
    } // 비활성화

    public void updatePreferences(Set<CounselingField> preferredFields, CounselingStyle preferredStyle) {
        if (preferredFields != null) {
            this.preferredFields.clear();
            this.preferredFields.addAll(preferredFields);
        }

        if (preferredStyle != null) {
            this.preferredStyle = preferredStyle;
        }
    }

    public boolean isSpeaker() {
        return this.waitingType == InitiatorType.SPEAKER;
    }

    public boolean isListener() {
        return this.waitingType == InitiatorType.LISTENER;
    }
}
