package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.review.service.ReviewService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final MatchingService matchingService;
    private final ReviewService reviewService;
    private final PointService pointService;

    @Override
    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileDetail(Long userId) {
        User user = userService.findUserById(userId);
        Profile profile = findProfileByUserId(userId);

        return buildProfileDetailResponse(profile, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileDetailById(Long profileId) {
        Profile profile = findProfileById(profileId);
        User user = profile.getUser();

        return buildProfileDetailResponse(profile, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileSimpleResponse getProfileSimple(Long userId) {
        User user = userService.findUserById(userId);
        Profile profile = findProfileByUserId(userId);
        Double averageRating = reviewService.getAverageRatingByUserId(userId);

        return ProfileSimpleResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .nickname(profile.getNickname())
                .profileImage(profile.getProfileImage())
                .totalCounselingCount(profile.getCounselingCount())
                .averageRating(averageRating)
                .build();
    }

    @Override
    public ProfileResponse createProfile(Long userId, ProfileCreateRequest request) {
        User user = userService.findUserById(userId);

        if (profileRepository.findByUserId(userId).isPresent()) {
            throw new CustomException(ProfileErrorCode.PROFILE_ALREADY_EXIST);
        }

        validateDuplicateNickname(request.getNickname());
        validateEntranceTime(request.getEntranceTime());

        Profile profile = Profile.builder()
                .user(user)
                .nickname(request.getNickname())
                .profileImage(request.getProfileImage())
                .department(request.getDepartment())
                .entranceTime(request.getEntranceTime())
                .graduation(request.isGraduation())
                .build();

        profileRepository.save(profile);
        user.updateRole(RoleType.ROLE_PROFILE);

        return ProfileResponse.of(profile.getId(), profile.getNickname(), "프로필이 생성되었습니다.");
    }

    @Override
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userService.findUserById(userId);
        Profile profile = getOrCreateProfile(user);

        if (request.getNickname() != null && !request.getNickname().equals(profile.getNickname())) {
            validateDuplicateNickname(request.getNickname());
            profile.updateNickname(request.getNickname());
        }
        // 이걸 여기 두는 게 맞나?
        if (request.getProfileImage() != null) {
            profile.updateProfileImage(request.getProfileImage());
        }
        if (request.getDepartment() != null) {
            profile.updateDepartment(request.getDepartment());
        }
        if (request.getEntranceTime() != null) {
            validateEntranceTime(request.getEntranceTime());
            profile.updateEntranceTime(request.getEntranceTime());
        }
        if (request.getGraduation() != null) {
            profile.updateGraduation(request.getGraduation());
        }

        return ProfileResponse.of(profile.getId(), profile.getNickname(), "프로필이 업데이트되었습니다.");
    }

    @Override
    public void incrementCounselingCount(Long userId) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        profile.incrementCounselingCount();
    }

    @Override
    public void updateResponseTime(Long userId, Integer responseTime) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        profile.updateResponseTime(responseTime);
    }

    @Override
    public void updateResponseTimes(Long userId, List<Integer> responseTimes) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        profile.addMultipleResponseTimes(responseTimes);
    }

    private Profile findProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    @Override
    public Profile findProfileById(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private void validateDuplicateNickname(String nickname) {
        if (profileRepository.existsByNickname(nickname)) {
            throw new CustomException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // 조회 아니면 생성
    private Profile getOrCreateProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile newProfile = Profile.builder()
                            .user(user)
                            .build();
                    return profileRepository.save(newProfile);
                });
    }

    private ProfileDetailResponse buildProfileDetailResponse(Profile profile, User user) {
        Long userId = user.getId();
        Long profileId = profile.getId();

        List<ReviewResponse> recentReviews = reviewService.getRecentReviewsByUserId(userId, 5);
        Double averageRating = reviewService.getAverageRatingByUserId(userId);
        Map<String, Integer> tagCounts = reviewService.getTagCountsByProfileId(profileId);
        Map<String, Integer> categoryMatchCounts = matchingService.getCategoryCountsByUserId(userId);

        int points = pointService.getCurrentBalance(userId);

        return ProfileDetailResponse.builder()
                .id(profileId)
                .userId(userId)
                .nickname(profile.getNickname())
                .profileImage(profile.getProfileImage())
                .department(profile.getDepartment())
                .entranceTime(profile.getEntranceTime())
                .graduation(profile.isGraduation())
                .totalCounselingCount(profile.getCounselingCount())
                .avgResponseTime(profile.getAvgResponseTime())
                .averageRating(averageRating)
                .tagCounts(tagCounts)
                .points(points)
                .categoryCounts(categoryMatchCounts)
                .reviews(recentReviews)
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private void validateEntranceTime(Integer entranceTime) {
        int currentYear = LocalDate.now().getYear();
        if (entranceTime < 1950 || entranceTime > currentYear + 1) {
            throw new CustomException(ProfileErrorCode.INVALID_ENTRANCE_TIME);
        }
    }

}
