package com.mindmate.mindmate_server.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileUpdateRequest {
    private String nickname;
    private String profileImage;
    private String department;
    private LocalDateTime entranceTime;
    private Boolean graduation;
}

