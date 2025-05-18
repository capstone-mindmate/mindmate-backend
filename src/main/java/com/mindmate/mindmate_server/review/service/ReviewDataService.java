package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.domain.Tag;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewDataService {
    private final ReviewRepository reviewRepository;

    public ReviewDataService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getRecentReviewsByUserId(Long userId, int limit) {
        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(
                userId,
                PageRequest.of(0, limit)
        );

        return recentReviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getAverageRatingByUserId(Long userId) {
        return reviewRepository.calculateAverageRatingByRevieweeId(userId)
                .orElse(0.0);
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getTagCountsByProfileId(Long profileId) {
        List<Object[]> tagCountResults = reviewRepository.countTagsByProfileId(profileId);
        Map<String, Integer> tagCounts = new HashMap<>();

        for (Object[] result : tagCountResults) {
            Tag tag = (Tag) result[0];
            Integer count = ((Long) result[1]).intValue();
            tagCounts.put(tag.getContent(), count);
        }

        return tagCounts;
    }
}