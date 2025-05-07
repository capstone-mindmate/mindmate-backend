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
    private LocalDateTime createdAt;

    public static WaitingUserResponse of(WaitingUser waitingUser) {

        String nickname = null;
        String profileImage = null;
        String department = null;

        if (!waitingUser.isAnonymous()) {
            nickname = waitingUser.getWaitingUser().getProfile().getNickname();
            profileImage = waitingUser.getWaitingUser().getProfile().getProfileImage().getImageUrl();
        }

        if (waitingUser.getMatching().isShowDepartment()) {
            department = waitingUser.getWaitingUser().getProfile().getDepartment();
        }

        return WaitingUserResponse.builder()
                .id(waitingUser.getId())
                .waitingUserId(waitingUser.getWaitingUser().getId())
                .waitingUserNickname(nickname)
                .waitingUserDepartment(department)
                .waitingUserEntranceTime(waitingUser.getWaitingUser().getProfile().getEntranceTime())
                .waitingUserGraduation(waitingUser.getWaitingUser().getProfile().isGraduation())
                .waitingUserCounselingCount(waitingUser.getWaitingUser().getProfile().getCounselingCount())
                .waitingUserProfileImage(profileImage)
                .message(waitingUser.getMessage())
                .createdAt(waitingUser.getCreatedAt())
                .build();
    }
}