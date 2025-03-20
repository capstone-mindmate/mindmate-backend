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
public interface MatchingRepository extends JpaRepository<Matching, Long> {

    // 사용자가 생성한 매칭방 목록?

    Page<Matching> findByStatusOrderByCreatedAtDesc(MatchingStatus status, Pageable pageable);

    Page<Matching> findByStatusAndCategoryOrderByCreatedAtDesc(
            MatchingStatus status, MatchingCategory category, Pageable pageable);

    Page<Matching> findByStatusAndCreatorRoleOrderByCreatedAtDesc(
            MatchingStatus status, InitiatorType creatorRole, Pageable pageable);

    @Query("SELECT mr FROM Matching mr WHERE mr.status = 'OPEN' AND mr.creator.profile.department = :department ORDER BY mr.createdAt DESC")
    Page<Matching> findOpenMatchingsByDepartment(
            @Param("department") String department, Pageable pageable);

    int countByCreatorAndStatus(User creator, MatchingStatus status);

    @Query("SELECT mr FROM Matching mr JOIN mr.creator u JOIN u.profile p " +
            "WHERE mr.status = :status AND mr.creatorRole = :role " +
            "AND mr.category = :category AND p.department = :department " +
            "ORDER BY mr.createdAt ASC")
    List<Matching> findByStatusRoleCategoryAndDepartment(
            @Param("status") MatchingStatus status,
            @Param("role") InitiatorType role,
            @Param("category") MatchingCategory category,
            @Param("department") String department);

    Page<Matching> findByAcceptedUserAndStatusOrderByMatchedAtDesc(
            User acceptedApplicant, MatchingStatus status, Pageable pageable);

    Page<Matching> findByCreatorAndStatusOrderByMatchedAtDesc(
            User creator, MatchingStatus status, Pageable pageable);

    List<Matching> findByStatusAndCreatorRoleAndCategoryOrderByCreatedAt(
            MatchingStatus status,
            InitiatorType creatorRole,
            MatchingCategory category);

    @Query("SELECT mr FROM Matching mr JOIN mr.creator u JOIN u.profile p " +
            "WHERE mr.status = :status AND mr.category = :category " +
            "AND p.department = :department ORDER BY mr.createdAt DESC")
    Page<Matching> findByStatusAndCategoryAndDepartment(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            @Param("department") String department,
            Pageable pageable);
}