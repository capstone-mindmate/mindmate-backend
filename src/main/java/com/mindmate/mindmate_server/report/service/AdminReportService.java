package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportDetailResponse;
import com.mindmate.mindmate_server.report.dto.ReportStatisticsResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AdminReportService {
    Page<ReportDetailResponse> getReports(int page, int size, ReportTarget target, ReportReason reason);

    ReportDetailResponse getReportDetail(Long reportId);

    ReportStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate);
}
