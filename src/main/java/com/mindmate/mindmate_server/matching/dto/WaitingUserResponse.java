package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.WaitingStatus;
import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import com.mindmate.mindmate_server.review.domain.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WaitingUserResponse {
    private Long id;
    private Long waitingUserId;
    private String waitingUserNickname;
    private String waitingUserDepartment;
    private Integer waitingUserEntranceTime;
    private Boolean waitingUserGraduation;
    private int waitingUserCounselingCount;
    private String waitingUserProfileImage;
    private String message;
    private WaitingStatus status;
//    private List<Tag> evaluationTags; // 이걸 보여줘야되나
    private LocalDateTime createdAt;

    public static WaitingUserResponse of(WaitingUser waitingUser) {

        String nickname = null;
        String profileImage = null;
        String department = null;

        if (!waitingUser.isAnonymous()) {
            nickname = waitingUser.getWaitingUser().getProfile().getNickname();
            profileImage = waitingUser.getWaitingUser().getProfile().getProfileImage();
        }

        if (waitingUser.getMatching().isShowDepartment()) {
            department = waitingUser.getWaitingUser().getProfile().getDepartment();
        }

//        List<Tag> tagNames = waitingUser.getWaitingUser().getProfile().getEvaluationTags().stream()
//                .map(EvaluationTag::getTagContent)
//                .toList();

        return WaitingUserResponse.builder()
                .id(waitingUser.getId())
                .waitingUserId(waitingUser.getWaitingUser().getId())
                .waitingUserNickname(waitingUser.getWaitingUser().getProfile().getNickname())
                .waitingUserDepartment(waitingUser.getWaitingUser().getProfile().getDepartment())
                .waitingUserEntranceTime(waitingUser.getWaitingUser().getProfile().getEntranceTime())
                .waitingUserGraduation(waitingUser.getWaitingUser().getProfile().isGraduation())
                .waitingUserCounselingCount(waitingUser.getWaitingUser().getProfile().getCounselingCount())
                .waitingUserProfileImage(waitingUser.getWaitingUser().getProfile().getProfileImage())
                .message(waitingUser.getMessage())
//                .status(waitingUser.getStatus())
                .createdAt(waitingUser.getCreatedAt())
//                .evaluationTags(tagNames)
                .build();
    }
}