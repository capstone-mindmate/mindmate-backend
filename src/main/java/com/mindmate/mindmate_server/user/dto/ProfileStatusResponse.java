package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileStatusResponse {
    private String status;
    private String message;
    private RoleType currentRole;
    private boolean hasListenerProfile;
    private boolean hasSpeakerProfile;

    public static ProfileStatusResponse of(String status, String message, RoleType currentRole,
                                           boolean hasListenerProfile, boolean hasSpeakerProfile) {
        return ProfileStatusResponse.builder()
                .status(status)
                .message(message)
                .currentRole(currentRole)
                .hasListenerProfile(hasListenerProfile)
                .hasSpeakerProfile(hasSpeakerProfile)
                .build();
    }
}
