package com.mindmate.mindmate_server.report.domain;

import com.mindmate.mindmate_server.global.entity.BaseTimeEntity;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reportReason;

    @Column(columnDefinition = "TEXT")
    private String additionalComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportTarget reportTarget;

    @Column(nullable = false)
    private String targetId;

    @Builder
    public Report(User reporter, User reportedUser, ReportReason reportReason, String additionalComment, ReportTarget reportTarget, String targetId) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reportReason = reportReason;
        this.additionalComment = additionalComment;
        this.reportTarget = reportTarget;
        this.targetId = targetId;
    }
}
