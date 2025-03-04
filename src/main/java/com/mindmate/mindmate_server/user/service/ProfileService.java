package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.*;

public interface ProfileService {

    ListenerProfileResponse getListenerProfile(Long profileId);
    SpeakerProfileResponse getSpeakerProfile(Long profileId);

    ProfileResponse createListenerProfile(Long userId, ListenerProfileRequest request);

    ProfileResponse createSpeakerProfile(Long userId, SpeakerProfileRequest request);

    ListenerProfileResponse updateListenerProfile(Long profileId, ListenerProfileUpdateRequest request);

    SpeakerProfileResponse updateSpeakerProfile(Long profileId, SpeakerProfileUpdateRequest request);

    ListenerProfileResponse updateListenerCertification(Long profileId, CertificationUpdateRequest request);

    ProfileStatusResponse switchRole(Long userId, RoleType targetRole);

    void updateAverageRating(Long profileId, RoleType roleType, Float newRating);

    void updateCounselingCount(Long profileId, RoleType roleType);

    void updateResponseTime(Long profileId, Integer responseTime);

//    void deleteProfile(Long profileId); // user 계정 삭제 시 삭제 되도록 만글기

}
