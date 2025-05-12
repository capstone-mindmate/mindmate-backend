package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitingUserRepository extends JpaRepository<WaitingUser, Long> {

    List<WaitingUser> findByMatchingOrderByCreatedAtDesc(Matching matching);
    Optional<WaitingUser> findByMatchingAndWaitingUser(Matching matching, User waitingUser);

    @Query("SELECT ma FROM WaitingUser ma " +
            "JOIN FETCH ma.waitingUser u " +
            "JOIN FETCH u.profile p " +
            "WHERE ma.matching = :matching " +
            "ORDER BY ma.createdAt DESC")
    Page<WaitingUser> findByMatchingWithWaitingUserProfile(@Param("matching") Matching matching, Pageable pageable);

    @Query("SELECT w FROM WaitingUser w JOIN w.matching m " +
            "WHERE w.waitingUser.id = :userId AND m.status = :status " +
            "ORDER BY w.createdAt DESC")
    Page<WaitingUser> findByWaitingUserIdAndMatchingStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") MatchingStatus status,
            Pageable pageable);


}
