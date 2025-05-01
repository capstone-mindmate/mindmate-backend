package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ProfileReviewSummaryResponse;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ReviewService {

    ReviewResponse createReview(Long userId, ReviewRequest request);

    Page<ReviewResponse> getProfileReviews(Long profileId, int page, int size, String sortType);

    ReviewResponse getReview(Long reviewId);

    List<ReviewResponse> getChatRoomReviews(Long chatRoomId);

    boolean canReview(Long userId, Long chatRoomId);

    ProfileReviewSummaryResponse getProfileReviewSummary(Long profileId);

    Review findReviewById(Long reviewId);
}
