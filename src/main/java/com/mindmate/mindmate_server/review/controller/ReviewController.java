package com.mindmate.mindmate_server.review.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.review.dto.ProfileReviewSummaryResponse;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/reviews")
@Tag(name = "Reviews", description = "리뷰 관련 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 생성", description = "채팅방에 대한 리뷰를 생성합니다.")
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.createReview(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "리뷰 작성 가능 여부 확인", description = "특정 채팅방에 대해 리뷰 작성 가능 여부를 확인합니다.")
    @GetMapping("/can-review/{chatRoomId}")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long chatRoomId) {

        boolean canReview = reviewService.canReview(userPrincipal.getUserId(), chatRoomId);
        return ResponseEntity.ok(Map.of("canReview", canReview));
    }

    @Operation(summary = "채팅방 리뷰 목록 조회", description = "특정 채팅방의 모든 리뷰를 조회합니다.")
    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<List<ReviewResponse>> getChatRoomReviews(
            @PathVariable Long chatRoomId) {

        List<ReviewResponse> responses = reviewService.getChatRoomReviews(chatRoomId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "리뷰 상세 조회", description = "특정 리뷰의 상세 정보를 조회합니다.")
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @PathVariable Long reviewId) {

        ReviewResponse response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 리뷰 목록 조회", description = "특정 프로필의 리뷰 목록을 페이지 단위로 조회합니다.")
    @GetMapping("/profile/{profileId}")
    public ResponseEntity<Page<ReviewResponse>> getProfileReviews(
            @PathVariable Long profileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sortType) {

        Page<ReviewResponse> responses = reviewService.getProfileReviews(
                profileId, page, size, sortType);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "프로필 리뷰 요약 정보 조회", description = "특정 프로필의 리뷰 요약 정보를 조회합니다.")
    @GetMapping("/profile/{profileId}/summary")
    public ResponseEntity<ProfileReviewSummaryResponse> getProfileReviewSummary(
            @PathVariable Long profileId) {

        ProfileReviewSummaryResponse summary = reviewService.getProfileReviewSummary(profileId);
        return ResponseEntity.ok(summary);
    }
}