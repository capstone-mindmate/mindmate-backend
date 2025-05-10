package com.mindmate.mindmate_server.report.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
import com.mindmate.mindmate_server.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(
        name = "신고",
        description = "사용자가 다른 사용자, 매칭, 채팅방, 리뷰 등을 신고할 수 있는 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    @Operation(
            summary = "신고 접수",
            description = """
                사용자가 다른 사용자, 매칭, 채팅방, 리뷰 등 다양한 대상을 신고할 수 있습니다.
                
                [유효성 검사]
                - 이미 동일한 대상을 신고한 경우 중복 신고 불가
                - 자기 자신은 신고할 수 없음
            """
    )
    @PostMapping
    public ResponseEntity<ReportResponse> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReportRequest request) {
        ReportResponse response = reportService.createReport(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
