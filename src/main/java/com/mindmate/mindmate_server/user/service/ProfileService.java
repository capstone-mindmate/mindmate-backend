package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
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

    void updateResponseTime(Long userId, Integer responseTime);

    void addEvaluationTags(Long userId, Set<String> tags);

    // 인기 프로필??
}
