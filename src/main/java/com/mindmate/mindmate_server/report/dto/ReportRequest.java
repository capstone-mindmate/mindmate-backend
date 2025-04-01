package com.mindmate.mindmate_server.report.dto;

import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {
    @NotNull
    private Long reportedUserId;

    @NotNull
    private ReportReason reportReason;

    private String additionalComment;

    @NotNull
    private ReportTarget reportTarget;

    @NotNull
    private String targetId;
}
