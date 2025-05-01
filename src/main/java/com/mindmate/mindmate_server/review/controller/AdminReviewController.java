package com.mindmate.mindmate_server.review.controller;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.review.domain.TagType;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.service.AdminReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminReviewController {
    private final AdminReviewService adminReviewService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @Min(1) @Max(5) Integer minRating,
            @RequestParam(required = false) @Min(1) @Max(5) Integer maxRating,
            @RequestParam(required = false) Boolean reported) {

        return ResponseEntity.ok(adminReviewService.getReviews(page, size, minRating, maxRating, reported));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        adminReviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

}