package com.mindmate.mindmate_server.matching.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matchings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Matching extends BaseTimeEntity {
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
    private MatchingType type;

    @Enumerated(EnumType.STRING)
    private MatchingStatus status;

    @Enumerated(EnumType.STRING)
    private InitiatorType initiator; // 요청 주체

    private String rejectionReason;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "chat_room_id")
    private String chatRoomId;

    @ElementCollection
    @CollectionTable(name = "matching_requested_fields",
            joinColumns = @JoinColumn(name = "matching_id"))
    @Enumerated(EnumType.STRING)
    private Set<CounselingField> requestedFields = new HashSet<>();

    @Builder
    public Matching(SpeakerProfile speakerProfile, ListenerProfile listenerProfile,
                    MatchingType type, Set<CounselingField> requestedFields,
                    InitiatorType initiator) {
        this.speakerProfile = speakerProfile;
        this.listenerProfile = listenerProfile;
        this.type = type;
        this.status = MatchingStatus.REQUESTED;
        this.requestedFields = requestedFields != null ? requestedFields : new HashSet<>();
        this.initiator = initiator;
    }

    public void accept() {
        this.status = MatchingStatus.ACCEPTED;
        this.matchedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = MatchingStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void complete() {
        this.status = MatchingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = MatchingStatus.CANCELED;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}
