package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.report.service.ReportService;
import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRedisRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewRedisRepository reviewRedisRepository;
    private final ReportService reportService;
    private final ReviewService reviewService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(int page, int size, Integer minRating, Integer maxRating, Boolean reported) {

        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new CustomException(ReviewErrorCode.INVALID_RATING_VALUE);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (reported != null && reported) {
            List<Long> reportedReviewIds = reportService.findReportedReviewIds();
            return reviewRepository.findReviewsWithFiltersAndReportCheck(minRating, maxRating, reportedReviewIds, pageable)
                    .map(ReviewResponse::from);
        } else {
            return reviewRepository.findReviewsWithFilters(minRating, maxRating, pageable)
                    .map(ReviewResponse::from);
        }
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewService.findReviewById(reviewId);

        Profile reviewedProfile = review.getReviewedProfile();

        adjustProfileMetricsAfterReviewDeletion(reviewedProfile, review);

        reviewRepository.delete(review);
        reviewRedisRepository.deleteAllProfileCaches(reviewedProfile.getId());
    }

    private void adjustProfileMetricsAfterReviewDeletion(Profile profile, Review review) {
        profile.decrementCounselingCount();

        profile.subtractRating(review.getRating());

        for (EvaluationTag tag : review.getReviewTags()) {
            reviewRedisRepository.decrementTagCount(profile.getId(), tag.getTagContent().getContent());
        }
    }
}