package com.mindmate.mindmate_server.report.dto;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.user.domain.Profile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportDetailResponse {
    private Long id;

    private Long reporterId;
    private String reporterName;
    private String reporterImage;
    private int reporterReportCount;

    private Long reportedUserId;
    private String reportedUserName;
    private String reportedUserImage;
    private int reportedUserReportCount;

    private ReportReason reportReason;
    private ReportTarget reportTarget;
    private Long targetId;

    private String additionalComment;
    private LocalDateTime createdAt;

    public static ReportDetailResponse from(Report report) {
        Profile reporterProfile = report.getReporter().getProfile();
        Profile reportedUserProfile = report.getReportedUser().getProfile();

        return ReportDetailResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(reporterProfile != null ? reporterProfile.getNickname() : report.getReporter().getEmail())
                .reporterImage(reporterProfile != null ? reporterProfile.getProfileImage().getImageUrl() : null)
                .reporterReportCount(report.getReporter().getReportCount())
                .reportedUserId(report.getReportedUser().getId())
                .reportedUserName(reportedUserProfile != null ? reportedUserProfile.getNickname() : report.getReportedUser().getEmail())
                .reportedUserImage(reportedUserProfile != null ? reportedUserProfile.getProfileImage().getImageUrl() : null)
                .reportedUserReportCount(report.getReportedUser().getReportCount())
                .reportReason(report.getReportReason())
                .reportTarget(report.getReportTarget())
                .targetId(report.getTargetId())
                .additionalComment(report.getAdditionalComment())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
