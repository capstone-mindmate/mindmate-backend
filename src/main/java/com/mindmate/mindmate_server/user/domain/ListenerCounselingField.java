package com.mindmate.mindmate_server.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListenerCounselingField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listener_profile_id")
    private ListenerProfile listenerProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounselingField field;

    @Builder
    public ListenerCounselingField(ListenerProfile listenerProfile, CounselingField field) {
        this.listenerProfile = listenerProfile;
        this.field = field;
    }
}
