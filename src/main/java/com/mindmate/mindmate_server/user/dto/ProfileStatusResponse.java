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
}
