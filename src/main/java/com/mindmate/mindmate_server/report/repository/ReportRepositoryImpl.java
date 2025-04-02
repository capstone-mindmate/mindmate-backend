package com.mindmate.mindmate_server.report.repository;

import com.mindmate.mindmate_server.report.domain.QReport;
import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    /**
     * 신고 목록 확인
     * @param target 신고 배경(매칭, 채팅방, 프로필0
     * @param reason 신고 이유
     * @param pageable
     * @return
     */
    @Override
    public Page<Report> findReportsWithFilters(ReportTarget target, ReportReason reason, Pageable pageable) {
        QReport report = QReport.report;

        BooleanBuilder builder = new BooleanBuilder();
        if (target != null) {
            builder.and(report.reportTarget.eq(target));
        }
        if (reason != null) {
            builder.and(report.reportReason.eq(reason));
        }

        List<Report> reports = queryFactory
                .selectFrom(report)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(report.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(report)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(reports, pageable, total);
    }

    @Override
    public Map<ReportReason, Long> countByReasonBetween(LocalDateTime start, LocalDateTime end) {
        QReport report = QReport.report;

        List<Tuple> results = queryFactory
                .select(report.reportReason, report.count())
                .from(report)
                .where(report.createdAt.between(start, end))
                .groupBy(report.reportReason)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, ReportReason.class),
                        tuple -> tuple.get(1, Long.class)
                ));
    }

    @Override
    public Map<ReportTarget, Long> countByTargetBetween(LocalDateTime start, LocalDateTime end) {
        QReport report = QReport.report;

        List<Tuple> results = queryFactory
                .select(report.reportTarget, report.count())
                .from(report)
                .where(report.createdAt.between(start, end))
                .groupBy(report.reportTarget)
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, ReportTarget.class),
                        tuple -> tuple.get(1, Long.class)
                ));
    }
}
