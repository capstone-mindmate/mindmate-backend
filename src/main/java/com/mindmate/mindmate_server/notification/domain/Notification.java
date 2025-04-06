package com.mindmate.mindmate_server.notification.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String type;

    private Long relatedEntityId;

    private boolean read = false;

    @Builder
    public Notification(Long userId, String title, String content, String type, Long relatedEntityId, boolean read) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.relatedEntityId = relatedEntityId;
        this.read = read;
    }
}