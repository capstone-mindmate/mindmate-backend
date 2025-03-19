package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.WaitingStatus;
import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingUserResponse {
    private Long id; // 무슨?
    private Long waitingUserId;
    private String waitingUserNickname;
    private String waitingUserDepartment;
    private Integer waitingUserEntranceTime;
    private Boolean waitingUserGraduation;
    private int waitingUserCounselingCount;
    private String waitingUserProfileImage;
    private String message;
    private WaitingStatus status;
    private List<String> evaluationTags;
    private LocalDateTime createdAt;

    public static WaitingUserResponse of(WaitingUser application) {
        // 지원자의 평가 태그 목록 가져오기
        List<String> tagNames = application.getWaitingUser().getProfile().getEvaluationTags().stream()
                .map(tag -> tag.getTagContent())
                .toList();

        return WaitingUserResponse.builder()
                .id(application.getId())
                .waitingUserId(application.getWaitingUser().getId())
                .waitingUserNickname(application.getWaitingUser().getProfile().getNickname())
                .waitingUserDepartment(application.getWaitingUser().getProfile().getDepartment())
                .waitingUserEntranceTime(application.getWaitingUser().getProfile().getEntranceTime())
                .waitingUserGraduation(application.getWaitingUser().getProfile().isGraduation())
                .waitingUserCounselingCount(application.getWaitingUser().getProfile().getCounselingCount())
                .waitingUserProfileImage(application.getWaitingUser().getProfile().getProfileImage())
                .message(application.getMessage())
//                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .evaluationTags(tagNames)
                .build();
    }
}