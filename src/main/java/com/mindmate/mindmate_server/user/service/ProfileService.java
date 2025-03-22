package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;


public interface ProfileService {

    /**
     * 사용자 ID로 상세 프로필 조회 - 마이페이지, 관리자 페이지
     */
    ProfileDetailResponse getProfileDetail(Long userId);

    /**
     * 프로필 ID로 상세 프로필 조회 - 프로필 목록?에서 볼 때
     */
    ProfileDetailResponse getProfileDetailById(Long profileId);

    /**
     * 사용자 ID로 간소화된 프로필 조회 - 대기열에서 볼 때
     */
    ProfileSimpleResponse getProfileSimple(Long userId);

    /**
     * 프로필 생성
     */
    ProfileResponse createProfile(Long userId, ProfileCreateRequest request);

    /**
     * 프로필 업데이트
     */
    ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);

    /**
     * 상담 횟수 증가
     */
    void incrementCounselingCount(Long userId);

    /**
     * 평균 응답 시간 업데이트
     */
    void updateResponseTime(Long userId, Integer responseTime);

    // 인기 프로필??
}
