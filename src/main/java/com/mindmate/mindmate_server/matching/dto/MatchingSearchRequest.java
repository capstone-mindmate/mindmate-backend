package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingSearchRequest {
    private String keyword;

    // 여기는 필터링이라 선택
    private MatchingCategory category;
    private String department;
    private InitiatorType requiredRole;
}