package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.review.dto.ReviewReplyRequest;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(Long userId, ReviewRequest request);

    ReviewResponse createReviewReply(Long userId, ReviewReplyRequest request);

    Page<ReviewResponse> getProfileReviews(Long profileId, int page, int size, String sortType);

    ReviewResponse getReview(Long reviewId);

    List<ReviewResponse> getChatRoomReviews(Long chatRoomId);

    boolean canReview(Long userId, Long chatRoomId);
}
