package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingResponse {
    private Long id;
    private String title;
    private String description;
    private MatchingCategory category;
    private InitiatorType creatorRole;
    private InitiatorType requiredRole;
    private String creatorNickname;
    private String creatorDepartment;
    private String creatorProfileImage;
    private int applicationCount;
    private MatchingType matchingType;
    private Long chatRoomId;
    private LocalDateTime createdAt;

    public static MatchingResponse of(Matching matching) {
        return MatchingResponse.builder()
                .id(matching.getId())
                .title(matching.getTitle())
                .description(matching.getDescription())
                .category(matching.getCategory())
                .creatorRole(matching.getCreatorRole())
                .requiredRole(matching.getRequiredRole())
                .creatorNickname(matching.getCreator().getProfile().getNickname())
                .creatorDepartment(matching.getCreator().getProfile().getDepartment())
                .creatorProfileImage(matching.getCreator().getProfile().getProfileImage())
                .applicationCount(matching.getWaitingUsersCount())
                .matchingType(matching.getType())
                .chatRoomId(matching.getChatRoom().getId())
                .createdAt(matching.getCreatedAt())
                .build();
    }
}
