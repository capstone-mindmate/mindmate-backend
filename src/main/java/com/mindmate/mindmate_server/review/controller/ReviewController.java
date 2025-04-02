package com.mindmate.mindmate_server.review.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.review.dto.ProfileReviewSummaryResponse;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.service.ReviewService;
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
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.createReview(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<List<ReviewResponse>> getChatRoomReviews(
            @PathVariable Long chatRoomId) {

        List<ReviewResponse> responses = reviewService.getChatRoomReviews(chatRoomId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @PathVariable Long reviewId) {

        ReviewResponse response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/can-review/{chatRoomId}")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long chatRoomId) {

        boolean canReview = reviewService.canReview(userPrincipal.getUserId(), chatRoomId);
        return ResponseEntity.ok(Map.of("canReview", canReview));
    }

    @GetMapping("/profile/{profileId}/summary")
    public ResponseEntity<ProfileReviewSummaryResponse> getProfileReviewSummary(
            @PathVariable Long profileId) {

        ProfileReviewSummaryResponse summary = reviewService.getProfileReviewSummary(profileId);
        return ResponseEntity.ok(summary);
    }
}