package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PopularMatchingCategoryResponse {

    private MatchingCategory matchingCategory;
}
