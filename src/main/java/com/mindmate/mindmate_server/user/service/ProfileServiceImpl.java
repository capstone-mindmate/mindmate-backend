package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.util.SecurityUtil;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.*;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.repository.ListenerRepository;
import com.mindmate.mindmate_server.user.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserService userService;
    private final ListenerRepository listenerRepository;
    private final SpeakerRepository speakerRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public ListenerProfileResponse getListenerProfile(Long profileId) {
        ListenerProfile profile = findListenerProfile(profileId);

        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(profile.getUser().getId(), PageRequest.of(0, 5));
        Double averageRating = reviewRepository.calculateAverageRatingByRevieweeId(profile.getUser().getId())
                .orElse(0.0);

        return ListenerProfileResponse.builder()
                .id(profile.getId())
                .nickname(profile.getNickname())
                .profileImage(profile.getProfileImage())
                .createdAt(profile.getCreatedAt())
                .totalCounselingCount(profile.getCounselingCount()) // 엔티티에 추가하기
                .averageRating(averageRating)
                .counselingStyle(profile.getCounselingStyle())
                .counselingFields(profile.getCounselingFields().stream()
                        .map(ListenerCounselingField::getField)
                        .collect(Collectors.toList()))
                .avgResponseTime(profile.getAvgResponseTime())
                .availableTimes(profile.getAvailableTimes())
                .badgeStatus(profile.getBadgeStatus())
                .reviews(recentReviews.stream()
                        .map(review -> ReviewResponse.builder()
                                .id(review.getId())
                                .content(review.getContent())
                                .rating(review.getRating())
                                .createdAt(review.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakerProfileResponse getSpeakerProfile(Long profileId) {
        SpeakerProfile profile = findSpeakerProfile(profileId);

        List<Review> recentReviews = reviewRepository.findRecentReviewsByRevieweeId(profile.getUser().getId(), PageRequest.of(0, 5));
        Double averageRating = reviewRepository.calculateAverageRatingByRevieweeId(profile.getUser().getId())
                .orElse(0.0);

        return SpeakerProfileResponse.builder()
                .id(profile.getId())
                .nickname(profile.getNickname())
                .profileImage(profile.getProfileImage())
                .createdAt(profile.getCreatedAt())
                .totalCounselingCount(profile.getCounselingCount())
                .averageRating(averageRating)
                .preferredStyle(profile.getPreferredCounselingStyle())
                .reviews(recentReviews.stream()
                        .map(review -> ReviewResponse.builder()
                                .id(review.getId())
                                .content(review.getContent())
                                .rating(review.getRating())
                                .reply(review.getReply())
                                .createdAt(review.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public ProfileResponse createListenerProfile(ListenerProfileRequest request) {
        User user = userService.findUserById(SecurityUtil.getCurrentUser().getId());
        validateUniqueNickname(request.getNickname());

        ListenerProfile profile = ListenerProfile.builder()
                .user(user)
                .nickname(request.getNickname())
                .counselingStyle(request.getCounselingStyle())
                .build();

        request.getCounselingFields().forEach(profile::addCounselingField);
        profile.addAvailableTimes(request.getAvailableTimes()); // json형태로 받았을 때

        ListenerProfile savedProfile = listenerRepository.save(profile);
        user.updateRole(RoleType.ROLE_LISTENER);

        return ProfileResponse.builder()
                .id(savedProfile.getId())
                .nickname(savedProfile.getNickname())
                .role(RoleType.ROLE_LISTENER)
                .message("리스너 프로필이 생성되었습니다")
                .build();
    }

    @Override
    public ProfileResponse createSpeakerProfile(SpeakerProfileRequest request) {
        User user = userService.findUserById(SecurityUtil.getCurrentUser().getId());
        validateUniqueNickname(request.getNickname());

        SpeakerProfile profile = SpeakerProfile.builder()
                .user(user)
                .nickname(request.getNickname())
                .preferredCounselingStyle(request.getPreferredCounselingStyle())
                .build();

        SpeakerProfile savedProfile = speakerRepository.save(profile);
        user.updateRole(RoleType.ROLE_SPEAKER);

        return ProfileResponse.builder()
                .id(savedProfile.getId())
                .nickname(savedProfile.getNickname())
                .role(RoleType.ROLE_SPEAKER)
                .message("스피커 프로필이 생성되었습니다")
                .build();
    }

    @Override
    @Transactional
    public ListenerProfileResponse updateListenerProfile(Long profileId, ListenerProfileUpdateRequest request) {
        ListenerProfile profile = listenerRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        if (request.getNickname() != null) {
            validateUniqueNickname(request.getNickname());
            profile.updateNickname(request.getNickname());
        }
        if (request.getProfileImage() != null) {
            profile.updateProfileImage(request.getProfileImage());
        }
        if (request.getCounselingStyle() != null) {
            profile.updateCounselingStyle(request.getCounselingStyle());
        }
        if (request.getCounselingFields() != null) {
            profile.updateCounselingFields(request.getCounselingFields());
        }
        if (request.getAvailableTimes() != null) {
            profile.addAvailableTimes(request.getAvailableTimes());
        }

        return getListenerProfile(profile.getId());
    }

    @Override
    @Transactional
    public SpeakerProfileResponse updateSpeakerProfile(Long profileId, SpeakerProfileUpdateRequest request) {
        SpeakerProfile profile = speakerRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));

        if (request.getNickname() != null) {
            validateUniqueNickname(request.getNickname());
            profile.updateNickname(request.getNickname());
        }
        if (request.getProfileImage() != null) {
            profile.updateProfileImage(request.getProfileImage());
        }
        if (request.getCounselingStyle() != null) {
            profile.updateCounselingStyle(request.getCounselingStyle());
        }

        return getSpeakerProfile(profile.getId());
    }

    @Override
    public ProfileStatusResponse switchRole(RoleType targetRole) {
        User user = userService.findUserById(SecurityUtil.getCurrentUser().getId());
        validateRoleTransition(user.getCurrentRole(), targetRole);

        boolean hasListenerProfile = user.getListenerProfile() != null;
        boolean hasSpeakerProfile = user.getSpeakerProfile() != null;

        if (targetRole == RoleType.ROLE_LISTENER && !hasListenerProfile) {
            return ProfileStatusResponse.builder()
                    .status("PROFILE_REQUIRED")
                    .message("리스너 프로필 작성이 필요합니다")
                    .currentRole(user.getCurrentRole())
                    .hasListenerProfile(hasListenerProfile)
                    .hasSpeakerProfile(hasSpeakerProfile)
                    .build();
        }

        if (targetRole == RoleType.ROLE_SPEAKER && !hasSpeakerProfile) {
            return ProfileStatusResponse.builder()
                    .status("PROFILE_REQUIRED")
                    .message("스피커 프로필 작성이 필요합니다")
                    .currentRole(user.getCurrentRole())
                    .hasListenerProfile(hasListenerProfile)
                    .hasSpeakerProfile(hasSpeakerProfile)
                    .build();
        }

        user.updateRole(targetRole);
        return ProfileStatusResponse.builder()
                .status("SUCCESS")
                .message("역할이 변경되었습니다.")
                .currentRole(targetRole)
                .hasListenerProfile(hasListenerProfile)
                .hasSpeakerProfile(hasSpeakerProfile)
                .build();
    }

    @Override // 리뷰 끝나고 실행
    public void updateAverageRating(Long profileId, RoleType roleType, Float newRating) {
        // requestbody로 값들 받아야겠죠? 수정해야겠따..
        Long totalReviews = reviewRepository.countReviewsByRevieweeId(profileId);

        if (roleType == RoleType.ROLE_LISTENER) {
            findListenerProfile(profileId).updateAverageRating(newRating, totalReviews);
        } else {
            findSpeakerProfile(profileId).updateAverageRating(newRating, totalReviews);
        }
    }

    @Override // 상담 끝나고 실행
    public void updateCounselingCount(Long profileId, RoleType roleType) {
        if (roleType == RoleType.ROLE_LISTENER) {
            findListenerProfile(profileId).incrementCounselingCount();
        } else {
            findSpeakerProfile(profileId).incrementCounselingCount();
        }
    }

    @Override // 상담 끝나고?
    public void updateResponseTime(Long profileId, Integer responseTime) {
        findListenerProfile(profileId).updateAverageResponseTime(responseTime);
    }

    private void validateRoleTransition(RoleType currentRole, RoleType targetRole) {
        if (currentRole == targetRole) {
            throw new CustomException(ProfileErrorCode.SAME_ROLE_TRANSITION);
        }

        if (targetRole == RoleType.ROLE_UNVERIFIED || targetRole == RoleType.ROLE_USER || targetRole == RoleType.ROLE_ADMIN) {
            throw new CustomException(ProfileErrorCode.INVALID_ROLE_TRANSITION);
        }
    }

    private void validateUniqueNickname(String nickname) {
        // 수정할 때 자기 이름도 포함되나
        if (listenerRepository.existsByNickname(nickname) || speakerRepository.existsByNickname(nickname)) {
            throw new CustomException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private ListenerProfile findListenerProfile(Long profileId) {
        return listenerRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private SpeakerProfile findSpeakerProfile(Long profileId) {
        return speakerRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

}
