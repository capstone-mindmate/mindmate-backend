package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r WHERE r.reviewedProfile.user.id = :revieweeId ORDER BY r.createdAt DESC")
    List<Review> findRecentReviewsByRevieweeId(@Param("revieweeId") Long revieweeId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewedProfile.user.id = :revieweeId")
    Optional<Double> calculateAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);


    List<Review> findByChatRoom(ChatRoom chatRoom);

    boolean existsByChatRoomAndReviewer(ChatRoom chatRoom, User reviewer);


}
