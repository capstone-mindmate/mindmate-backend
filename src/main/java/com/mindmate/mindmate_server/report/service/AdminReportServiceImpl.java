package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportDetailResponse;
import com.mindmate.mindmate_server.report.dto.ReportStatisticsResponse;
import com.mindmate.mindmate_server.report.dto.UnsuspendRequest;
import com.mindmate.mindmate_server.report.repository.ReportRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {
    private final ReportRepository reportRepository;
    private final UserService userService;
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
    @Transactional
    public void suspendUser(Long userId, int reportCount, Duration duration) {
        User user = userService.findUserById(userId);
        user.setReportCount(reportCount);
        user.suspend(duration);
        userService.save(user);
    }

    @Override
    @Transactional
    public void unsuspendUser(Long userId, UnsuspendRequest request) {
        User user = userService.findUserById(userId);
        user.setReportCount(request.getReportCount());
        user.unsuspend();
        userService.save(user);
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
