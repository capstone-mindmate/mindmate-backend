package com.mindmate.mindmate_server.matching.controller;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
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
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping
    public ResponseEntity<MatchingCreateResponse> createMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MatchingCreateRequest request) {
        MatchingCreateResponse matching = matchingService.createMatching(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(matching);
    }

    @GetMapping
    public ResponseEntity<Page<MatchingResponse>> getMatchings(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) MatchingCategory category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) InitiatorType requiredRole) {
        Page<MatchingResponse> matchings = matchingService.getMatchings(pageable, category, department, requiredRole);
        return ResponseEntity.ok(matchings);
    }

    @GetMapping("/{matchingId}")
    public ResponseEntity<MatchingDetailResponse> getMatchingDetail(
            @PathVariable Long matchingId) {
        MatchingDetailResponse matching = matchingService.getMatchingDetail(matchingId);
        return ResponseEntity.ok(matching);
    }

    @PostMapping("/{matchingId}/apply")
    public ResponseEntity<Long> applyForMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @Valid @RequestBody WaitingUserRequest request) {
        Long waitingUserId = matchingService.applyForMatching(userPrincipal.getUserId(), matchingId, request);
        return ResponseEntity.ok(waitingUserId);
    }

    @PutMapping("/{matchingId}")
    public ResponseEntity<MatchingDetailResponse> updateMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingUpdateRequest request) {
        MatchingDetailResponse updatedMatching = matchingService.updateMatching(
                userPrincipal.getUserId(), matchingId, request);
        return ResponseEntity.ok(updatedMatching);
    }

    @GetMapping("/{matchingId}/waitingUsers")
    public ResponseEntity<List<WaitingUserResponse>> getWaitingUsers(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId) {
        List<WaitingUserResponse> waitingUsers = matchingService.getWaitingUsers(userPrincipal.getUserId(), matchingId);
        return ResponseEntity.ok(waitingUsers);
    }

    @PostMapping("/{matchingId}/accept/{waitingId}")
    public ResponseEntity<Long> acceptMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId,
            @PathVariable Long waitingId) {
        Long matchedId = matchingService.acceptMatching(userPrincipal.getUserId(), matchingId, waitingId);
        return ResponseEntity.ok(matchedId);
    }

    @DeleteMapping("/waitingUsers/{waitingUserId}")
    public ResponseEntity<Void> cancelWaiting(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long waitingUserId) {
        matchingService.cancelWaiting(userPrincipal.getUserId(), waitingUserId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{matchingId}/close")
    public ResponseEntity<Void> closeMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long matchingId) {
        matchingService.closeMatching(userPrincipal.getUserId(), matchingId);
        return ResponseEntity.ok().build();
    }

    // 생성한거
    @GetMapping("/history/created")
    public ResponseEntity<Page<MatchingResponse>> getCreatedMatchingHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "matchedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MatchingResponse> matchingHistory = matchingService.getUserMatchingHistory(userPrincipal.getUserId(), pageable, false);
        return ResponseEntity.ok(matchingHistory);
    }

    // 참여한거
    @GetMapping("/history/participated")
    public ResponseEntity<Page<MatchingResponse>> getParticipatedMatchingHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "matchedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MatchingResponse> matchingHistory = matchingService.getUserMatchingHistory(userPrincipal.getUserId(), pageable, true);
        return ResponseEntity.ok(matchingHistory);
    }

    @PostMapping("/auto")
    public ResponseEntity<Long> autoMatching(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody AutoMatchingRequest request) {
        Long matchingId = matchingService.autoMatchApply(
                userPrincipal.getUserId(), request);
        return ResponseEntity.ok(matchingId);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MatchingResponse>> searchMatchings(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MatchingCategory category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) InitiatorType requiredRole) {
        MatchingSearchRequest request = new MatchingSearchRequest(keyword, category, department, requiredRole);
        Page<MatchingResponse> matchings = matchingService.searchMatchings(pageable, request);
        return ResponseEntity.ok(matchings);
    }
}
