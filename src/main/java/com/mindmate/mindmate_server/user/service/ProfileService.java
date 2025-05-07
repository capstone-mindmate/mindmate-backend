package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;


public interface ProfileService {

    ProfileDetailResponse getProfileDetail(Long userId);

    ProfileDetailResponse getProfileDetailById(Long profileId);

    ProfileSimpleResponse getProfileSimple(Long userId);

    ProfileResponse createProfile(Long userId, ProfileCreateRequest request);

    ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);

    void incrementCounselingCount(Long userId);

    void updateAvgRating(Long userId, double rating);

    void updateResponseTimes(Long userId, List<Integer> responseTimes);

    Profile findProfileById(Long profileId);

    ProfileDetailResponse getMyProfileDetail(Long userId);
}
