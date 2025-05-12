package com.mindmate.mindmate_server.matching.controller;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matchings")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "매칭 관련 API")
public class MatchingController {

    private final MatchingService matchingService;

    @Operation(summary = "매칭 생성", description = "새로운 매칭을 생성합니다. 매칭 생성과 동시에 채팅방이 자동으로 생성됩니다.")
    @PostMapping
    public ResponseEntity<MatchingCreateResponse> createMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MatchingCreateRequest request) {
        MatchingCreateResponse matching = matchingService.createMatching(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(matching);
    }

    @Operation(summary = "매칭 목록 조회", description = "필터링 옵션을 적용하여 매칭 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<MatchingResponse>> getMatchings(
            @Parameter(description = "페이지네이션 정보")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) MatchingCategory category,
            @RequestParam(required = false) String department,
            @Parameter(description = "요청하는 역할 필터 (화자/청자)")
            @RequestParam(required = false) InitiatorType requiredRole) {
        Page<MatchingResponse> matchings = matchingService.getMatchings(pageable, category, department, requiredRole);
        return ResponseEntity.ok(matchings);
    }

    @Operation(summary = "매칭 검색", description = "키워드, 카테고리, 학과, 역할 등으로 매칭을 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<Page<MatchingResponse>> searchMatchings(
            @Parameter(description = "페이지네이션 정보")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MatchingCategory category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) InitiatorType requiredRole) {
        MatchingSearchRequest request = new MatchingSearchRequest(keyword, category, department, requiredRole);
        Page<MatchingResponse> matchings = matchingService.searchMatchings(pageable, request);
        return ResponseEntity.ok(matchings);
    }

    @Operation(summary = "매칭 상세 조회", description = "특정 매칭의 상세 정보를 조회합니다.")
    @GetMapping("/{matchingId}")
    public ResponseEntity<MatchingDetailResponse> getMatchingDetail(
            @PathVariable Long matchingId) {
        MatchingDetailResponse matching = matchingService.getMatchingDetail(matchingId);
        return ResponseEntity.ok(matching);
    }

    @Operation(summary = "매칭 정보 수정", description = "매칭 생성자만 매칭 정보를 수정할 수 있습니다.")
    @PutMapping("/{matchingId}")
    public ResponseEntity<MatchingDetailResponse> updateMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingUpdateRequest request) {
        MatchingDetailResponse updatedMatching = matchingService.updateMatching(
                userPrincipal.getUserId(), matchingId, request);
        return ResponseEntity.ok(updatedMatching);
    }

    @Operation(summary = "매칭 취소", description = "매칭 생성자가 매칭을 취소하거나 종료합니다.")
    @PatchMapping("/{matchingId}")
    public ResponseEntity<Void> closeMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId) {
        matchingService.cancelMatching(userPrincipal.getUserId(), matchingId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "내가 생성한 매칭 내역 조회", description = "사용자가 생성한 매칭 내역을 조회합니다.")
    @GetMapping("/creator")
    public ResponseEntity<Page<MatchingResponse>> getCreatedMatchingHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "페이지네이션 정보")
            @PageableDefault(size = 10, sort = "matchedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MatchingResponse> matchingHistory = matchingService.getCreatedMatchings(userPrincipal.getUserId() ,pageable);
        return ResponseEntity.ok(matchingHistory);
    }

    @Operation(summary = "내가 신청한 매칭 내역 조회", description = "사용자가 신청한 매칭 내역을 조회합니다.")
    @GetMapping("/applications")
    public ResponseEntity<Page<MatchingResponse>> getAppliedMatchingHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "페이지네이션 정보")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MatchingResponse> matchingHistory = matchingService.getAppliedMatchings(userPrincipal.getUserId(), pageable);
        return ResponseEntity.ok(matchingHistory);
    }

    @Operation(summary = "매칭 신청", description = "특정 매칭에 참여 신청을 합니다.")
    @PostMapping("/{matchingId}/applications")
    public ResponseEntity<Long> applyForMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @Valid @RequestBody WaitingUserRequest request) {
        Long waitingUserId = matchingService.applyForMatching(userPrincipal.getUserId(), matchingId, request);
        return ResponseEntity.ok(waitingUserId);
    }

    @Operation(summary = "매칭 신청자 목록 조회", description = "매칭 생성자만 신청자 목록을 조회할 수 있습니다.")
    @GetMapping("/{matchingId}/waiting-users")
    public ResponseEntity<Page<WaitingUserResponse>> getWaitingUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @Parameter(description = "페이지네이션 정보")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = userPrincipal.getUserId();
        Page<WaitingUserResponse> waitingUsers = matchingService.getWaitingUsers(userId, matchingId, pageable);
        return ResponseEntity.ok(waitingUsers);
    }

    @Operation(summary = "매칭 수락", description = "매칭 생성자가 대기 중인 사용자의 신청을 수락합니다.")
    @PostMapping("/{matchingId}/{waitingId}/acceptance")
    public ResponseEntity<Long> acceptMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @PathVariable Long waitingId) {
        Long matchedId = matchingService.acceptMatching(userPrincipal.getUserId(), matchingId, waitingId);
        return ResponseEntity.ok(matchedId);
    }

    @Operation(summary = "매칭 신청 취소", description = "사용자가 자신의 매칭 신청을 취소합니다.")
    @DeleteMapping("/waiting-users/{waitingUserId}")
    public ResponseEntity<Void> cancelWaiting(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long waitingUserId) {
        matchingService.cancelWaiting(userPrincipal.getUserId(), waitingUserId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "자동 매칭 신청", description = "랜덤 매칭을 신청합니다. 즉시 매칭이 이루어집니다.")
    @PostMapping("/auto")
    public ResponseEntity<Long> autoMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AutoMatchingRequest request) {
        Long matchingId = matchingService.autoMatchApply(
                userPrincipal.getUserId(), request);
        return ResponseEntity.ok(matchingId);
    }
}
