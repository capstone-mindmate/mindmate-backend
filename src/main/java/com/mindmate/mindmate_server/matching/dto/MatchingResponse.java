package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingResponse {
    private Long id;
    private String title;
    private String description;
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
                .department(department)
                .creatorRole(matching.getCreatorRole())
                .build();
    }
}