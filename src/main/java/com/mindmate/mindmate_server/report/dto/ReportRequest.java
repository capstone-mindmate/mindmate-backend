package com.mindmate.mindmate_server.report.dto;

import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
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
    private Long targetId;
}
