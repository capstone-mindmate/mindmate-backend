package com.mindmate.mindmate_server.report.dto;

import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ReportStatisticsResponse {
    private long totalReports;
    private Map<ReportReason, Long> reportsByReason;
    private Map<ReportTarget, Long> reportsByTarget;
}
