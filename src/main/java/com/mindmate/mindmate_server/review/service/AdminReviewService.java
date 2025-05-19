package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;

public interface AdminReviewService {

    Page<ReviewResponse> getReviews(int page, int size, Integer minRating, Integer maxRating, Boolean reported);

    void deleteReview(Long reviewId);
}