package com.mindmate.mindmate_server.report.controller;

import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportDetailResponse;
import com.mindmate.mindmate_server.report.dto.ReportStatisticsResponse;
import com.mindmate.mindmate_server.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private final AdminReportService adminReportService;

    /**
     * 신고 목록 확인
     * target, reason 필터링에 따라 확인 가능 + pagination
     */
    @GetMapping
    public ResponseEntity<Page<ReportDetailResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReportTarget target,
            @RequestParam(required = false) ReportReason reason) {
        Page<ReportDetailResponse> reports = adminReportService.getReports(page, size, target, reason);
        return ResponseEntity.ok(reports);
    }

    /**
     * 개별 신고 확인
     * 해당 신고의 상세 내용 전부 확인
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminReportService.getReportDetail(reportId));
    }


    /**
     * 신고 통계
     * 특정 기간 동안의 Reason/Target 별 count 신고 데이터 확인
     */
    @GetMapping("/statistics")
    public ResponseEntity<ReportStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate endDate) {
        return ResponseEntity.ok(adminReportService.getStatistics(startDate, endDate));
    }
}
