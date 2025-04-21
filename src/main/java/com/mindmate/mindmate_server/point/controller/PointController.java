package com.mindmate.mindmate_server.point.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.point.dto.PointAddRequest;
import com.mindmate.mindmate_server.point.dto.PointSummaryResponse;
import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
import com.mindmate.mindmate_server.point.dto.PointUseRequest;
import com.mindmate.mindmate_server.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
@Tag(name = "포인트", description = "포인트 적립/사용 및 조회 API")
public class PointController {

    private final PointService pointService;

    @Operation(summary = "포인트 잔액 조회", description = "로그인한 사용자의 현재 포인트 잔액을 조회합니다.")
    @GetMapping("/balance")
    public ResponseEntity<Integer> getCurrentBalance(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pointService.getCurrentBalance(principal.getUserId()));
    }

    @Operation(summary = "포인트 적립 요청", description = "포인트를 적립합니다.")
    @PostMapping("/earn")
    public ResponseEntity<PointTransactionResponse> earnPoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PointAddRequest request) {
        return ResponseEntity.ok(pointService.addPoints(principal.getUserId(), request));
    }

    @Operation(summary = "포인트 사용 요청", description = "포인트를 사용합니다.")
    @PostMapping("/spend")
    public ResponseEntity<PointTransactionResponse> spendPoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PointUseRequest request) {
        return ResponseEntity.ok(pointService.usePoints(principal.getUserId(), request));
    }

    @Operation(summary = "포인트 거래내역 조회", description = "포인트 거래내역을 조회합니다.")
    @GetMapping("/history")
    public ResponseEntity<List<PointTransactionResponse>> getTransactionHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PointTransactionResponse> historyPage = pointService.getTransactionHistory(principal.getUserId(), PageRequest.of(page, size));
        return ResponseEntity.ok(historyPage.getContent());
    }

    @Operation(summary = "기간별 총 적립 포인트 조회", description = "특정 기간 동안 적립한 포인트의 총합을 조회합니다.")
    @GetMapping("/total-earned")
    public ResponseEntity<Integer> getTotalEarnedPoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(pointService.getTotalEarnedPointsInPeriod(principal.getUserId(), start, end));
    }

    @Operation(summary = "기간별 총 사용 포인트 조회", description = "특정 기간 동안 사용한 포인트의 총합을 조회합니다.")
    @GetMapping("/total-spent")
    public ResponseEntity<Integer> getTotalSpentPoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(pointService.getTotalSpentPointsInPeriod(principal.getUserId(), start, end));
    }

    @Operation(summary = "포인트 요약 정보 조회", description = "현재 잔액, 총 적립, 총 사용 정보를 조회합니다.")
    @GetMapping("/summary")
    public ResponseEntity<PointSummaryResponse> getPointSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(pointService.getUserPointSummary(principal.getUserId()));
    }
}
