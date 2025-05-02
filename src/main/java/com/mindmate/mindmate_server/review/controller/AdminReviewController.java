package com.mindmate.mindmate_server.review.controller;

import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.service.AdminReviewService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin Reviews", description = "관리자 리뷰 관리 API")
public class AdminReviewController {
    private final AdminReviewService adminReviewService;

    @GetMapping
    @Operation(summary = "리뷰 목록 조회", description = "필터링 옵션을 적용하여 모든 사용자의 리뷰 목록을 조회합니다.")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @Min(1) @Max(5) Integer minRating,
            @RequestParam(required = false) @Min(1) @Max(5) Integer maxRating,
            @Parameter(description = "신고된 리뷰만 보기")
            @RequestParam(required = false) Boolean reported) {

        return ResponseEntity.ok(adminReviewService.getReviews(page, size, minRating, maxRating, reported));
    }

    @Operation(summary = "리뷰 삭제", description = "관리자 권한으로 특정 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        adminReviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}