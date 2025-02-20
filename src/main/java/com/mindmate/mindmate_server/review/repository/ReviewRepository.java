package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.review.domain.Review;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r WHERE r.reviewee.id = :revieweeId ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsByRevieweeId(@Param("revieweeId") Long revieweeId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :revieweeId")
    Optional<Double> calculateAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);
}
