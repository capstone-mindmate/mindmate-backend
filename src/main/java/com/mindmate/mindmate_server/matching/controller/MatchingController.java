package com.mindmate.mindmate_server.matching.controller;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Matching", description = "매칭 관련 API")
public class MatchingController {
    private final MatchingService matchingService;

    @PostMapping("/auto/random")
    @Operation(summary = "자동 랜덤 매칭 요청", description = "자동 랜덤 매칭을 요청합니다.")
    public ResponseEntity<MatchingResponse> requestAutoRandomMatch(
            @Valid @RequestBody AutoRandomMatchRequest request) {

        MatchingResponse response = matchingService.autoRandomMatch(
                request.getProfileId(), request.getInitiatorType());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auto/format")
    @Operation(summary = "자동 형식 매칭 요청", description = "특정 조건에 맞는 자동 매칭을 요청합니다.")
    public ResponseEntity<MatchingResponse> requestAutoFormatMatch(
            @Valid @RequestBody AutoFormatMatchRequest request) {

        MatchingResponse response = matchingService.autoFormatMatch(
                request.getProfileId(), request.getInitiatorType(),
                request.getRequestedFields(), request.getPreferredStyle());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/manual")
    @Operation(summary = "수동 매칭 요청", description = "특정 상대방을 선택하여 매칭을 요청합니다.")
    public ResponseEntity<MatchingResponse> requestManualMatch(
            @Valid @RequestBody ManualMatchRequest request) {

        MatchingResponse response = matchingService.manualMatch(
                request.getInitiatorId(), request.getInitiatorType(),
                request.getRecipientId(), request.getRequestedFields());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchingId}/accept")
    @Operation(summary = "매칭 수락", description = "요청된 매칭을 수락합니다.")
    public ResponseEntity<MatchingResponse> acceptMatching(
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingActionRequest request) {

        MatchingResponse response = matchingService.acceptMatching(
                matchingId, request.getProfileId(), request.getProfileType());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchingId}/reject")
    @Operation(summary = "매칭 거절", description = "요청된 매칭을 거절합니다.")
    public ResponseEntity<MatchingResponse> rejectMatching(
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingRejectRequest request) {

        MatchingResponse response = matchingService.rejectMatching(
                matchingId, request.getProfileId(), request.getProfileType(), request.getReason());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchingId}/cancel")
    @Operation(summary = "매칭 취소", description = "요청한 매칭을 취소합니다.")
    public ResponseEntity<MatchingResponse> cancelMatching(
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingActionRequest request) {

        MatchingResponse response = matchingService.cancelMatching(
                matchingId, request.getProfileId(), request.getProfileType());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchingId}/complete")
    @Operation(summary = "매칭 완료", description = "진행 중인 매칭을 완료합니다.")
    public ResponseEntity<MatchingResponse> completeMatching(
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingActionRequest request) {

        MatchingResponse response = matchingService.completeMatching(
                matchingId, request.getProfileId(), request.getProfileType());
        return ResponseEntity.ok(response);
    }
}