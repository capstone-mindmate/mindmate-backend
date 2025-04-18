package com.mindmate.mindmate_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuspendedUserDTO {
    private Long userId;
    private String email;
    private String nickname;
    private int reportCount;
    private LocalDateTime suspensionEndTime;
    private String suspensionReason;
}
