package com.mindmate.mindmate_server.auth.dto;

import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private RoleType currentRole;
    private boolean hasListenerProfile;
    private boolean hasSpeakerProfile;
    private String message;
}
