package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MatchingRepositoryCustom {

    Page<Matching> getMatchingsWithFilters(MatchingStatus status, MatchingCategory category,
                                           String department, InitiatorType creatorRole,
                                           Long excludeUserId, List<Long> excludeMatchingIds,
                                           Pageable pageable);

    Page<Matching> searchMatchingsWithFilters(MatchingStatus status, String keyword,
                                              MatchingCategory category, String department,
                                              InitiatorType creatorRole, Long excludeUserId, List<Long> excludeMatchingIds,Pageable pageable);
}