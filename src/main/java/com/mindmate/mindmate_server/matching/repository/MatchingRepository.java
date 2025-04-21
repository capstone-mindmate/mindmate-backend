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

    // 사용자가 생성한 매칭방 목록?

    // 자동 매칭
    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.allowRandom = :allowRandom " +
            "AND m.creatorRole = :creatorRole AND (SELECT COUNT(w) FROM m.waitingUsers w) = 0")
    List<Matching> findByStatusAndAllowRandomAndCreatorRoleAndWaitingUsersIsEmpty(
            @Param("status") MatchingStatus status,
            @Param("allowRandom") boolean allowRandom,
            @Param("creatorRole") InitiatorType creatorRole
    );

    // 기본 매칭 조회 메서드
    @Query(value = "SELECT m FROM Matching m WHERE m.status = :status",
            countQuery = "SELECT COUNT(m) FROM Matching m WHERE m.status = :status")
    Page<Matching> findByStatusOrderByCreatedAtDesc(MatchingStatus status, Pageable pageable);

    // 역할별 필터링
    Page<Matching> findByStatusAndCreatorRoleOrderByCreatedAtDesc(
            MatchingStatus status, InitiatorType creatorRole, Pageable pageable);

    // 학과별 오픈 매칭 필터링
    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.creator.profile.department = :department ORDER BY m.createdAt DESC")
    Page<Matching> findOpenMatchingsByDepartment(
            @Param("status") MatchingStatus status,
            @Param("department") String department,
            Pageable pageable);

    // 카테고리별 필터링
    @Query("SELECT m FROM Matching m WHERE m.status = :status AND m.category = :category ORDER BY m.createdAt DESC")
    Page<Matching> findByStatusAndCategoryOrderByCreatedAtDesc(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            Pageable pageable);

    // 카테고리와 학과별 필터링
    @Query("SELECT m FROM Matching m JOIN m.creator u JOIN u.profile p " +
            "WHERE m.status = :status AND m.category = :category " +
            "AND p.department = :department ORDER BY m.createdAt DESC")
    Page<Matching> findByStatusAndCategoryAndDepartment(
            @Param("status") MatchingStatus status,
            @Param("category") MatchingCategory category,
            @Param("department") String department,
            Pageable pageable);

    // 사용자의 활성화된 매칭 수 카운트
    int countByCreatorAndStatus(User creator, MatchingStatus status);

    // 사용자의 매칭 이력 조회 (참여자로)
    Page<Matching> findByAcceptedUserAndStatusOrderByMatchedAtDesc(
            User acceptedApplicant, MatchingStatus status, Pageable pageable);

    // 사용자의 매칭 이력 조회 (생성자로)
    Page<Matching> findByCreatorAndStatusOrderByMatchedAtDesc(
            User creator, MatchingStatus status, Pageable pageable);

    // 검색
    @Query("SELECT m FROM Matching m LEFT JOIN m.creator u LEFT JOIN u.profile p " +
            "WHERE m.status = :status AND (" +
            "LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "(m.showDepartment = true AND LOWER(p.department) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "ORDER BY m.createdAt DESC")
    Page<Matching> searchByKeyword(
            @Param("status") MatchingStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

//    // 필터링 포함 검샏
//    @Query("SELECT DISTINCT m FROM Matching m " +
//            "LEFT JOIN m.creator u " +
//            "LEFT JOIN u.profile p " +
//            "WHERE m.status = :status AND (" +
//            "LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//            "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//            "(m.showDepartment = true AND LOWER(p.department) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
//            "AND (:category IS NULL OR m.category = :category) " +
//            "AND (:department IS NULL OR (m.showDepartment = true AND p.department = :department)) " +
//            "AND (:creatorRole IS NULL OR m.creatorRole = :creatorRole) " +
//            "ORDER BY m.createdAt DESC")
//    Page<Matching> searchByKeywordWithFilters(
//            @Param("status") MatchingStatus status,
//            @Param("keyword") String keyword,
//            @Param("category") MatchingCategory category,
//            @Param("department") String department,
//            @Param("creatorRole") InitiatorType creatorRole,
//            Pageable pageable);
}