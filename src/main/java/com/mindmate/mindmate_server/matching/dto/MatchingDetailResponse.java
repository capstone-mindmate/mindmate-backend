package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class MatchingDetailResponse {
    private Long id;
    private String title;
    private String description;
    private MatchingCategory category;
    private MatchingStatus status;
    private LocalDateTime createdAt;
    private int waitingCount;

    private boolean anonymous;
    private boolean showDepartment;
    private InitiatorType creatorRole;

    private Long creatorId;
    private String creatorNickname;
    private String creatorProfileImage;
    private String creatorDepartment;
    private int creatorCounselingCount; // 이거 보여줌?? 보여줘도 좋을 것 같은데
    private double creatorAvgRating;

    public static MatchingDetailResponse of(Matching matching) {
        boolean isAnonymous = matching.isAnonymous();

        Long creatorId = matching.getCreator().getId();

        String nickname = null;
        String profileImage = null;
        String department = null;
        int counselingCount = 0;
        double avgRating = 0.0;

        if(matching.getCreator().getProfile() != null) {

            if (!isAnonymous) {
                nickname = matching.getCreator().getProfile().getNickname();
                profileImage = matching.getCreator().getProfile().getProfileImage();
            }

            if (matching.isShowDepartment()) {
                department = matching.getCreator().getProfile().getDepartment();
            }

            counselingCount = matching.getCreator().getProfile().getCounselingCount();
            avgRating = matching.getCreator().getProfile().getAvgRating();
        }

        return MatchingDetailResponse.builder()
                .id(matching.getId())
                .title(matching.getTitle())
                .description(matching.getDescription())
                .category(matching.getCategory())
                .status(matching.getStatus())
                .createdAt(matching.getCreatedAt())
                .waitingCount(matching.getWaitingUsersCount())
                .anonymous(matching.isAnonymous())
                .showDepartment(matching.isShowDepartment())
                .creatorRole(matching.getCreatorRole())
                .creatorId(creatorId)
                .creatorNickname(nickname)
                .creatorProfileImage(profileImage)
                .creatorDepartment(department)
                .creatorCounselingCount(counselingCount)
                .creatorAvgRating(avgRating)
                .build();
    }
}
