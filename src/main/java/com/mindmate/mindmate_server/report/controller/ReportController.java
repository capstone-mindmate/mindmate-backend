package com.mindmate.mindmate_server.report.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
import com.mindmate.mindmate_server.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {
    private final ReportService reportService;
    /**
     * 신고하기
     */
    @PostMapping
    public ResponseEntity<ReportResponse> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid ReportRequest request) {
        ReportResponse response = reportService.createReport(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
