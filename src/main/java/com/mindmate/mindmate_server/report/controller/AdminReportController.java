package com.mindmate.mindmate_server.report.controller;

import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportDetailResponse;
import com.mindmate.mindmate_server.report.dto.ReportStatisticsResponse;
import com.mindmate.mindmate_server.report.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(
        name = "관리자 신고 관리",
        description = "사용자 신고 내역 조회, 상세 확인 및 통계 분석을 위한 관리자용 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/reports")
public class AdminReportController {
    private final AdminReportService adminReportService;

    @Operation(
            summary = "신고 목록 조회",
            description = """
                사용자 신고 목록을 페이지네이션하여 조회합니다.
                
                [필터링 옵션]
                - target: 신고 대상 유형(MATCHING, CHATROOM, PROFILE, REVIEW)으로 필터링
                - reason: 신고 사유(SPAM, ABUSE, INAPPROPRIATE 등)로 필터링
            """
    )
    @GetMapping
    public ResponseEntity<Page<ReportDetailResponse>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReportTarget target,
            @RequestParam(required = false) ReportReason reason) {
        Page<ReportDetailResponse> reports = adminReportService.getReports(page, size, target, reason);
        return ResponseEntity.ok(reports);
    }

    @Operation(
            summary = "신고 상세 조회",
            description = """
                특정 신고 건의 상세 정보를 조회합니다.
            """
    )
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminReportService.getReportDetail(reportId));
    }


    @Operation(
            summary = "신고 통계 조회",
            description = """
                지정된 기간 동안의 신고 통계를 조회합니다.
                
                [통계 정보]
                - 총 신고 건수
                - 신고 사유별 건수 분포
                - 신고 대상 유형별 건수 분포
                
                [기간 설정]
                - startDate: 시작일(미지정 시 1개월 전)
                - endDate: 종료일(미지정 시 오늘)
            """
    )
    @GetMapping("/statistics")
    public ResponseEntity<ReportStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate endDate) {
        return ResponseEntity.ok(adminReportService.getStatistics(startDate, endDate));
    }
}
