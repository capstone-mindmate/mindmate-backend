package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewRepositoryCustom {
    Page<Review> findReviewsWithFilters(Integer minRating, Integer maxRating, Pageable pageable);

    Page<Review> findReviewsWithFiltersAndReportCheck(Integer minRating, Integer maxRating,
                                                      List<Long> reportedIds, Pageable pageable);
}