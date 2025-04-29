package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {
    @Query("SELECT r FROM Review r WHERE r.reviewedProfile.user.id = :revieweeId ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsByRevieweeId(@Param("revieweeId") Long revieweeId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewedProfile.user.id = :revieweeId")
    Optional<Double> calculateAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);

    @EntityGraph(attributePaths = {"reviewTags", "reviewer", "reviewedProfile", "chatRoom"})
    List<Review> findByChatRoom(ChatRoom chatRoom);

    boolean existsByChatRoomAndReviewer(ChatRoom chatRoom, User reviewer);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.reviewTags WHERE r.reviewedProfile = :profile ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.reviewedProfile = :profile")
    Page<Review> findByReviewedProfileOrderByCreatedAtDesc(@Param("profile") Profile profile, Pageable pageable);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.reviewTags WHERE r.reviewedProfile = :profile ORDER BY r.rating DESC",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.reviewedProfile = :profile")
    Page<Review> findByReviewedProfileOrderByRatingDesc(Profile profile, Pageable pageable);

    @Query(value = "SELECT r FROM Review r JOIN FETCH r.reviewTags WHERE r.reviewedProfile = :profile ORDER BY r.rating ASC",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.reviewedProfile = :profile")
    Page<Review> findByReviewedProfileOrderByRatingAsc(Profile profile, Pageable pageable);


    @Query("SELECT COUNT(r) FROM Review r WHERE r.reviewedProfile = :profile")
    long countByReviewedProfile(@Param("profile") Profile profile);

    @Query("SELECT rt.tagContent, COUNT(rt) FROM EvaluationTag rt " +
            "JOIN rt.review r WHERE r.reviewedProfile = :profile " +
            "GROUP BY rt.tagContent")
    List<Object[]> countAllTagsByProfile(@Param("profile") Profile profile);

    @Query("SELECT et.tagContent, COUNT(et) FROM EvaluationTag et " +
            "JOIN et.review r WHERE r.reviewedProfile.id = :profileId " +
            "GROUP BY et.tagContent")
    List<Object[]> countTagsByProfileId(@Param("profileId") Long profileId);
}
