package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.*;
import org.springframework.transaction.annotation.Transactional;

public interface ProfileService {
    ListenerProfileResponse getListenerProfile(Long profileId);
    SpeakerProfileResponse getSpeakerProfile(Long profileId);

    ProfileResponse createListenerProfile(ListenerProfileRequest request);
    ProfileResponse createSpeakerProfile(SpeakerProfileRequest request);

    @Transactional
    ListenerProfileResponse updateListenerProfile(Long profileId, ListenerProfileUpdateRequest request);

    @Transactional
    SpeakerProfileResponse updateSpeakerProfile(Long profileId, SpeakerProfileUpdateRequest request);

    ProfileStatusResponse switchRole(RoleType targetRole);

    void updateAverageRating(Long profileId, RoleType roleType, Float newRating);

    void updateCounselingCount(Long profileId, RoleType roleType);

    void updateResponseTime(Long profileId, Integer responseTime);

//    void deleteProfile(Long profileId); // user 계정 삭제 시 삭제 되도록 만글기
}
