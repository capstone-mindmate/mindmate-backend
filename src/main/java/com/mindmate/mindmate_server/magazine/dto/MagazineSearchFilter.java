package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MagazineSearchFilter {
    private MatchingCategory category;
    private String keyword;
    private SortType sortBy;

    public enum SortType {
        POPULARITY, LATEST, OLDEST
    }
}
