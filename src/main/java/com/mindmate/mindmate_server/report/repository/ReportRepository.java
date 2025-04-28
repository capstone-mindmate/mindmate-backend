package com.mindmate.mindmate_server.report.repository;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {
    boolean existsByReporterAndReportedUserAndReportTargetAndTargetId(User reporter, User reportedUser, ReportTarget target, Long targetId);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT r.targetId FROM Report r WHERE r.reportTarget = com.mindmate.mindmate_server.report.domain.ReportTarget.REVIEW GROUP BY r.targetId")
    List<Long> findReportedReviewIds();
}
