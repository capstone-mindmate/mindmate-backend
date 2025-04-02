package com.mindmate.mindmate_server.matching.repository;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchingRepositoryCustom {
    Page<Matching> searchMatchingsWithFilters(MatchingStatus status, String keyword,
                                              MatchingCategory category, String department,
                                              InitiatorType creatorRole, Pageable pageable);
}