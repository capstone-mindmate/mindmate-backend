package com.mindmate.mindmate_server.report.repository;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

public interface ReportRepositoryCustom {
    Page<Report> findReportsWithFilters(ReportTarget target, ReportReason reason, Pageable pageable);
    Map<ReportReason, Long> countByReasonBetween(LocalDateTime start, LocalDateTime end);
    Map<ReportTarget, Long> countByTargetBetween(LocalDateTime start, LocalDateTime end);
}
