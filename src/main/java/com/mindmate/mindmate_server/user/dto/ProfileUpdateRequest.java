package com.mindmate.mindmate_server.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileUpdateRequest {
    private String nickname;
    private String profileImage;
}

