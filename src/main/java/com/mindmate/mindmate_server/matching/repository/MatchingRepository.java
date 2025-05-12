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

    Page<Matching> findByStatusAndCreatorIdNotAndIdNotInOrderByCreatedAtDesc(
            MatchingStatus status, Long creatorId, List<Long> excludeIds, Pageable pageable);

    Page<Matching> findByStatusAndCreatorRoleAndCreatorIdNotAndIdNotInOrderByCreatedAtDesc(
            MatchingStatus status, InitiatorType creatorRole, Long creatorId, List<Long> excludeIds, Pageable pageable);

    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.creator.profile.department = :department " +
            "AND m.creator.id != :creatorId AND m.id NOT IN :excludeIds ORDER BY m.createdAt DESC")
    Page<Matching> findOpenMatchingsByDepartmentAndCreatorIdNotAndIdNotIn(
            @Param("status") MatchingStatus status,
            @Param("department") String department,
            @Param("creatorId") Long creatorId,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable);

    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.category = :category " +
            "AND m.creator.id != :creatorId AND m.id NOT IN :excludeIds ORDER BY m.createdAt DESC")
    Page<Matching> findByStatusAndCategoryAndCreatorIdNotAndIdNotInOrderByCreatedAtDesc(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            @Param("creatorId") Long creatorId,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable);

    @Query("SELECT m FROM Matching m JOIN m.creator u JOIN u.profile p " +
            "WHERE m.status = :status AND m.category = :category " +
            "AND p.department = :department AND m.creator.id != :creatorId " +
            "AND m.id NOT IN :excludeIds ORDER BY m.createdAt DESC")
    Page<Matching> findByStatusAndCategoryAndDepartmentAndCreatorIdNotAndIdNotIn(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            @Param("department") String department,
            @Param("creatorId") Long creatorId,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable);
}