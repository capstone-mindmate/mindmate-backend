package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchingRepository extends JpaRepository<Matching, Long>, MatchingRepositoryCustom {

    Page<Matching> findByCreatorIdAndStatusOrderByCreatedAtDesc(Long creatorId, MatchingStatus status, Pageable pageable);

    @Query("SELECT m.category, COUNT(m) FROM Matching m " +
            "WHERE (m.creator.id = :userId OR m.acceptedUser.id = :userId) " +
            "AND m.status = 'MATCHED' " +
            "GROUP BY m.category")
    List<Object[]> countMatchingsByUserAndCategory(@Param("userId") Long userId);

    @Query("SELECT m.category FROM Matching m " +
            "WHERE m.status = 'MATCHED' " +
            "GROUP BY m.category " +
            "ORDER BY COUNT(m) DESC " +
            "LIMIT 1")
    MatchingCategory findMostPopularMatchingCategory();
}