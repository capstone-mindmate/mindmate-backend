package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MagazineCategoryStatistics {
    private MatchingCategory category;
    private int magazineCount;
    private int totalLikes;
}
