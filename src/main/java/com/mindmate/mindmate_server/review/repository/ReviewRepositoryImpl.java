package com.mindmate.mindmate_server.review.repository;

import com.mindmate.mindmate_server.review.domain.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Review> findReviewsWithFilters(Integer minRating, Integer maxRating, Pageable pageable) {
        QReview review = QReview.review;

        BooleanBuilder builder = new BooleanBuilder();

        if (minRating != null) {
            builder.and(review.rating.goe(minRating));
        }

        if (maxRating != null) {
            builder.and(review.rating.loe(maxRating));
        }

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(review.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(review)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(reviews, pageable, total);
    }

    @Override
    public Page<Review> findReviewsWithFiltersAndReportCheck(Integer minRating, Integer maxRating,
                                                             List<Long> reportedIds, Pageable pageable) {
        QReview review = QReview.review;

        BooleanBuilder builder = new BooleanBuilder();

        if (minRating != null) {
            builder.and(review.rating.goe(minRating));
        }

        if (maxRating != null) {
            builder.and(review.rating.loe(maxRating));
        }

        if (reportedIds != null && !reportedIds.isEmpty()) {
            builder.and(review.id.in(reportedIds));
        }

        List<Review> reviews = queryFactory
                .selectFrom(review)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(review.createdAt.desc())
                .fetch();

        long total = queryFactory
                .selectFrom(review)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(reviews, pageable, total);
    }
}