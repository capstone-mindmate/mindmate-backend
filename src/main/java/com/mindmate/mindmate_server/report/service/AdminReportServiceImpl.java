package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportDetailResponse;
import com.mindmate.mindmate_server.report.dto.ReportStatisticsResponse;
import com.mindmate.mindmate_server.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {
    private final ReportRepository reportRepository;
    private final ReportService reportService;

    @Override
    public Page<ReportDetailResponse> getReports(int page, int size, ReportTarget target, ReportReason reason) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Report> reports = reportRepository.findReportsWithFilters(target, reason, pageable);

        return reports.map(ReportDetailResponse::from);
    }

    @Override
    public ReportDetailResponse getReportDetail(Long reportId) {
        return ReportDetailResponse.from(reportService.findReportById(reportId));
    }

    @Override
    public ReportStatisticsResponse getStatistics(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        long totalReports = reportRepository.countByCreatedAtBetween(start, end);
        Map<ReportReason, Long> reportsByReason = reportRepository.countByReasonBetween(start, end);
        Map<ReportTarget, Long> reportsByTarget = reportRepository.countByTargetBetween(start, end);

        return new ReportStatisticsResponse(totalReports, reportsByReason, reportsByTarget);
    }
}
