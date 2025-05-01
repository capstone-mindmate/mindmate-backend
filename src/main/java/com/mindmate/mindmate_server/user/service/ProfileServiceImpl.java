package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.service.ReviewDataService;
import com.mindmate.mindmate_server.review.service.ReviewService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final MatchingService matchingService;
    private final ReviewDataService reviewService;
    private final PointService pointService;

    @Override
    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileDetail(Long userId) {
        Profile profile = profileRepository.findWithUserByUserId(userId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
        User user = profile.getUser();

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
        Profile profile = findProfileByUser(user);
        Double averageRating = reviewService.getAverageRatingByUserId(userId);

        return buildProfileSimpleResponse(profile, user, averageRating);
    }

    @Override
    public ProfileResponse createProfile(Long userId, ProfileCreateRequest request) {
        User user = userService.findUserById(userId);

        checkDuplicateProfile(userId);
        validateNickname(request.getNickname());
        validateEntranceTime(request.getEntranceTime());

        Profile profile = buildProfileFromRequest(user, request);

        profileRepository.save(profile);
        user.updateRole(RoleType.ROLE_PROFILE);

        return ProfileResponse.of(profile.getId(), profile.getNickname(), "프로필이 생성되었습니다.");
    }

    @Override
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userService.findUserById(userId);
        Profile profile = getOrCreateProfile(user);

        updateProfileFields(profile, request);

        return ProfileResponse.of(profile.getId(), profile.getNickname(), "프로필이 업데이트되었습니다.");
    }

    @Override
    public void incrementCounselingCount(Long userId) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        profile.incrementCounselingCount();
    }

    @Override
    public void updateAvgRating(Long userId, double rating) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        profile.updateRating(rating);
    }

    @Override
    public void updateResponseTimes(Long userId, List<Integer> responseTimes) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        profile.addMultipleResponseTimes(responseTimes);
    }

    private Profile findProfileByUser(User user) {
        return profileRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Profile findProfileById(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private void validateNickname(String nickname) {
        if (profileRepository.existsByNickname(nickname)) {
            throw new CustomException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void checkDuplicateProfile(Long userId){
        if (profileRepository.findByUserId(userId).isPresent()) {
            throw new CustomException(ProfileErrorCode.PROFILE_ALREADY_EXIST);
        }
    }

    private Profile getOrCreateProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile newProfile = Profile.builder()
                            .user(user)
                            .build();
                    return profileRepository.save(newProfile);
                });
    }

    private Profile buildProfileFromRequest(User user, ProfileCreateRequest request) {
        return Profile.builder()
                .user(user)
                .nickname(request.getNickname())
                .profileImage(request.getProfileImage())
                .department(request.getDepartment())
                .entranceTime(request.getEntranceTime())
                .graduation(request.isGraduation())
                .build();
    }

    private void updateProfileFields(Profile profile, ProfileUpdateRequest request) {
        if (request.getNickname() != null && !request.getNickname().equals(profile.getNickname())) {
            validateNickname(request.getNickname());
            profile.updateNickname(request.getNickname());
        }

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

    private ProfileSimpleResponse buildProfileSimpleResponse(Profile profile, User user, Double averageRating) {
        return ProfileSimpleResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .nickname(profile.getNickname())
                .profileImage(profile.getProfileImage())
                .totalCounselingCount(profile.getCounselingCount())
                .averageRating(averageRating)
                .build();
    }

    private void validateEntranceTime(Integer entranceTime) {
        int currentYear = LocalDate.now().getYear();
        if (entranceTime < 1950 || entranceTime > currentYear + 1) {
            throw new CustomException(ProfileErrorCode.INVALID_ENTRANCE_TIME);
        }
    }

}
