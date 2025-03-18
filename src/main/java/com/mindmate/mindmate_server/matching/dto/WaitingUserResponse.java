package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.WaitingStatus;
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
}