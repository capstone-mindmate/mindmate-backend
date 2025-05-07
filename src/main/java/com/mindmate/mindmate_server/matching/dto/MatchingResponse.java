package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingResponse {
    private Long id;
    private String title;
    private String description;
    private MatchingCategory category;
    private String department;
    private InitiatorType creatorRole;

    public static MatchingResponse of(Matching matching) {
        String department = null;
        if (matching.isShowDepartment() && matching.getCreator().getProfile() != null) {
            department = matching.getCreator().getProfile().getDepartment();
        }

        return MatchingResponse.builder()
                .id(matching.getId())
                .title(matching.getTitle())
                .description(matching.getDescription())
                .category(matching.getCategory())
                .department(department)
                .creatorRole(matching.getCreatorRole())
                .build();
    }
}