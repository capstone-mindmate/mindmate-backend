package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.ListenerProfileRequest;
import com.mindmate.mindmate_server.user.dto.ProfileResponse;
import com.mindmate.mindmate_server.user.dto.ProfileStatusResponse;
import com.mindmate.mindmate_server.user.dto.SpeakerProfileRequest;

public interface ProfileService {

    ProfileResponse createListenerProfile(ListenerProfileRequest request);

    ProfileResponse createSpeakerProfile(SpeakerProfileRequest request);

    ProfileStatusResponse switchRole(RoleType targetRole);

}
