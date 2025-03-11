package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.*;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Profile profile = getOrCreateProfile(user);

        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(
                userId,
                PageRequest.of(0, 5)
        );

        Double averageRating = reviewRepository.calculateAverageRatingByRevieweeId(userId)
                .orElse(0.0);

        return ProfileDetailResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(profile.getProfileImage())
                .department(user.getDepartment())
                .entranceTime(user.getEntranceTime())
                .graduation(user.isGraduation())
                .totalCounselingCount(profile.getCounselingCount())
                .avgResponseTime(profile.getAvgResponseTime())
                .averageRating(averageRating)
                .evaluationTags(profile.getEvaluationTags())
                .reviews(mapReviews(recentReviews))
                .createdAt(profile.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileDetailById(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        User user = profile.getUser();

        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(
                user.getId(),
                PageRequest.of(0, 5)
        );

        Double averageRating = reviewRepository.calculateAverageRatingByRevieweeId(user.getId())
                .orElse(0.0);

        return ProfileDetailResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImage(profile.getProfileImage())
                .department(user.getDepartment())
                .entranceTime(user.getEntranceTime())
                .graduation(user.isGraduation())
                .totalCounselingCount(profile.getCounselingCount())
                .avgResponseTime(profile.getAvgResponseTime())
                .averageRating(averageRating)
                .evaluationTags(profile.getEvaluationTags())
                .reviews(mapReviews(recentReviews))
                .createdAt(profile.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileSimpleResponse getProfileSimple(Long userId) {
        User user = userService.findUserById(userId);
        Profile profile = getOrCreateProfile(user);

        Double averageRating = reviewRepository.calculateAverageRatingByRevieweeId(userId)
                .orElse(0.0);

        return ProfileSimpleResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .nickname(user.getNickname())
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

        // 닉네임 겹침 여부? 유저에ㅓㅅ?

        Profile profile = Profile.builder()
                .user(user)
                .profileImage(request.getProfileImage())
                .build();

        profileRepository.save(profile);

        return ProfileResponse.of(profile.getId(), "프로필이 생성되었습니다.");
    }

    @Override
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userService.findUserById(userId);
        Profile profile = getOrCreateProfile(user);

        // todo : 닉네임 중복 확인 로직

        if (request.getProfileImage() != null) {
            profile.updateProfileImage(request.getProfileImage());
        }

        return ProfileResponse.of(profile.getId(), "프로필이 업데이트되었습니다.");
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
    public void addEvaluationTags(Long userId, Set<String> tags) {
        Profile profile = getOrCreateProfile(userService.findUserById(userId));
        for (String tag : tags) {
            profile.addEvaluationTag(tag);
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
