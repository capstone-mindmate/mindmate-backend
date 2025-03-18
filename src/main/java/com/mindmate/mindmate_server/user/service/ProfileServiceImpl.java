package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final ReviewRepository reviewRepository;

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
        Double averageRating = getAverageRating(userId);

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
        // todo: 입학 연도 유효성 검사

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

    private Profile findProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private Profile findProfileById(Long profileId) {
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

    // 사용자의 최근 리뷰 5개 조회
    private List<ReviewResponse> getRecentReviews(Long userId) {
        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(
                userId,
                PageRequest.of(0, 5)
        );

        return mapReviews(recentReviews);
    }

    // 사용자 평균 평점 계산
    private Double getAverageRating(Long userId) {
        return reviewRepository.calculateAverageRatingByRevieweeId(userId)
                .orElse(0.0);
    }

    private ProfileDetailResponse buildProfileDetailResponse(Profile profile, User user) {
        Long userId = user.getId();
        List<ReviewResponse> recentReviews = getRecentReviews(userId);
        Double averageRating = getAverageRating(userId);

        return ProfileDetailResponse.builder()
                .id(profile.getId())
                .userId(userId)
                .nickname(profile.getNickname())
                .profileImage(profile.getProfileImage())
                .department(profile.getDepartment())
                .entranceTime(profile.getEntranceTime())
                .graduation(profile.isGraduation())
                .totalCounselingCount(profile.getCounselingCount())
                .avgResponseTime(profile.getAvgResponseTime())
                .averageRating(averageRating)
//                .evaluationTags(profile.getEvaluationTags())
                .reviews(recentReviews)
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private List<ReviewResponse> mapReviews(List<Review> reviews) {
        return reviews.stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .content(review.getContent())
                        .rating(review.getRating())
                        .reply(review.getReply())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 프로필 검색은 필요없?
}
