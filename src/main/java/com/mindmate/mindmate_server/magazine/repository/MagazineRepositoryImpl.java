package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.magazine.domain.QMagazine;
import com.mindmate.mindmate_server.magazine.dto.MagazineCategoryStatistics;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.dto.MagazineSearchFilter;
import com.mindmate.mindmate_server.user.domain.QProfile;
import com.mindmate.mindmate_server.user.domain.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MagazineRepositoryImpl implements MagazineRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    @Override
    public Page<MagazineResponse> findMagazinesWithFilters(MagazineSearchFilter filter, Pageable pageable) {
        QMagazine magazine = QMagazine.magazine;
        QUser author = QUser.user;
        QProfile profile = QProfile.profile;

        BooleanBuilder builder = new BooleanBuilder();
        // 1. 게시된 매거진 확인
        builder.and(magazine.magazineStatus.eq(MagazineStatus.PUBLISHED));

        // 2. 카테고리 필터 확인
        if (filter.getCategory() != null) {
            builder.and(magazine.category.eq(filter.getCategory()));
        }

        // 3. 키워드 필터 확인
        if (StringUtils.hasText(filter.getKeyword())) {
            builder.and(
                    magazine.title.containsIgnoreCase(filter.getKeyword())
                            .or(magazine.content.containsIgnoreCase(filter.getKeyword()))
            );
        }

        // 4. 정렬 조건 확인
        OrderSpecifier<?> orderSpecifier = getOrderSpecifier(filter.getSortBy(), magazine);

        List<Magazine> magazines = queryFactory
                .selectFrom(magazine)
                .join(magazine.author, author).fetchJoin()
                .join(author.profile, profile).fetchJoin()
                .leftJoin(magazine.images).fetchJoin()
                .where(builder)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<MagazineResponse> content = magazines.stream()
                .map(MagazineResponse::from)
                .collect(Collectors.toList());

        Long countResult = queryFactory
                .select(magazine.count())
                .from(magazine)
                .join(magazine.author, author)
                .where(builder)
                .fetchOne();

        long total = countResult != null ? countResult : 0L;


        return new PageImpl<>(content, pageable, total);


    }

    @Override
    public List<MagazineCategoryStatistics> getCategoryStatistics() {
        QMagazine magazine = QMagazine.magazine;

        List<Tuple> results = queryFactory
                .select(
                        magazine.category,
                        magazine.count(),
                        magazine.likeCount.sum()
                )
                .from(magazine)
                .where(magazine.magazineStatus.eq(MagazineStatus.PUBLISHED))
                .groupBy(magazine.category)
                .fetch();

        return results.stream()
                .map(tuple -> MagazineCategoryStatistics.builder()
                        .category(tuple.get(magazine.category))
                        .magazineCount(tuple.get(magazine.count()).intValue())
                        .totalLikes(tuple.get(magazine.likeCount.sum()) != null
                        ? tuple.get(magazine.likeCount.sum()).intValue() : 0)
                        .build())
                .collect(Collectors.toList());
    }

    private OrderSpecifier<?> getOrderSpecifier(MagazineSearchFilter.SortType sortType, QMagazine magazine) {
        if (sortType == null) {
            return magazine.createdAt.desc();
        }

        return switch (sortType) {
            case POPULARITY -> magazine.likeCount.desc();
            case OLDEST -> magazine.createdAt.asc();
            default -> magazine.createdAt.desc();
        };

    }
}
