package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.user.domain.QProfile;
import com.mindmate.mindmate_server.user.domain.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MatchingRepositoryImpl implements MatchingRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public MatchingRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<Matching> searchMatchingsWithFilters(MatchingStatus status, String keyword,
                                                     MatchingCategory category, String department,
                                                     InitiatorType creatorRole, Pageable pageable) {
        QMatching matching = QMatching.matching;
        QUser creator = QUser.user;
        QProfile profile = QProfile.profile;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(matching.status.eq(status));

        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordLower = keyword.toLowerCase();
            builder.and(
                    matching.title.toLowerCase().contains(keywordLower)
                            .or(matching.description.toLowerCase().contains(keywordLower))
                            .or(matching.showDepartment.isTrue().and(profile.department.toLowerCase().contains(keywordLower)))
            );
        }

        if (category != null) {
            builder.and(matching.category.eq(category));
        }

        if (department != null) {
            builder.and(matching.showDepartment.isTrue())
                    .and(profile.department.eq(department));
        }

        if (creatorRole != null) {
            builder.and(matching.creatorRole.eq(creatorRole));
        }

        JPAQuery<Long> countQuery = queryFactory
                .select(matching.count())
                .from(matching)
                .leftJoin(matching.creator, creator)
                .leftJoin(creator.profile, profile)
                .where(builder);

        long total = countQuery.fetchOne();

        List<Matching> content = queryFactory
                .selectFrom(matching)
                .leftJoin(matching.creator, creator).fetchJoin()
                .leftJoin(creator.profile, profile).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifier(pageable, matching))
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private OrderSpecifier<?>[] getOrderSpecifier(Pageable pageable, QMatching matching) {
        if (!pageable.getSort().isEmpty()) {
            List<OrderSpecifier<?>> orders = new ArrayList<>();

            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

                switch (order.getProperty()) {
                    case "title":
                        orders.add(new OrderSpecifier<>(direction, matching.title));
                        break;
                    default:
                        orders.add(new OrderSpecifier<>(direction, matching.createdAt));
                }
            }

            return orders.toArray(new OrderSpecifier[0]);
        }

        return new OrderSpecifier[] { new OrderSpecifier<>(Order.DESC, matching.createdAt) };
    }
}
