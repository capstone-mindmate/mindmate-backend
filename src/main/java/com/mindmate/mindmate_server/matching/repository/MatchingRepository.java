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

    @Query(value = "SELECT m FROM Matching m WHERE m.status = :status",
            countQuery = "SELECT COUNT(m) FROM Matching m WHERE m.status = :status")
    Page<Matching> findByStatusOrderByCreatedAtDesc(MatchingStatus status, Pageable pageable);

    Page<Matching> findByStatusAndCreatorRoleOrderByCreatedAtDesc(
            MatchingStatus status, InitiatorType creatorRole, Pageable pageable);

    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.creator.profile.department = :department ORDER BY m.createdAt DESC")
    Page<Matching> findOpenMatchingsByDepartment(
            @Param("status") MatchingStatus status,
            @Param("department") String department,
            Pageable pageable);

    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.category = :category ORDER BY m.createdAt DESC")
    Page<Matching> findByStatusAndCategoryOrderByCreatedAtDesc(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            Pageable pageable);

    @Query("SELECT m FROM Matching m JOIN m.creator u JOIN u.profile p " +
            "WHERE m.status = :status AND m.category = :category " +
            "AND p.department = :department ORDER BY m.createdAt DESC")
    Page<Matching> findByStatusAndCategoryAndDepartment(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            @Param("department") String department,
            Pageable pageable);

    Page<Matching> findByCreatorIdAndStatusOrderByCreatedAtDesc(Long creatorId, MatchingStatus status, Pageable pageable);

    @Query("SELECT m.category, COUNT(m) FROM Matching m " +
            "WHERE (m.creator.id = :userId OR m.acceptedUser.id = :userId) " +
            "AND m.status = 'MATCHED' " +
            "GROUP BY m.category")
    List<Object[]> countMatchingsByUserAndCategory(@Param("userId") Long userId);

}