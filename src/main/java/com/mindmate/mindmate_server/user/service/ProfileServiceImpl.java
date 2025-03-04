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
    private final SecurityUtil securityUtil;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public ListenerProfileResponse getListenerProfile(Long profileId) {
        ListenerProfile profile = findListenerProfile(profileId);

        List<Review> recentReviews = reviewRepository.findRecentReviewsByUserIdAndRole(
                profile.getUser().getId(),
                RoleType.ROLE_LISTENER,
                PageRequest.of(0, 5)
        );
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
                .availableTimes(profile.getAvailableTime())
                .badgeStatus(profile.getBadgeStatus())
                .reviews(mapListenerReviews(recentReviews))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakerProfileResponse getSpeakerProfile(Long profileId) {
        SpeakerProfile profile = findSpeakerProfile(profileId);

        List<Review> recentReviews = reviewRepository.findRecentReviewsByUserIdAndRole(
                profile.getUser().getId(),
                RoleType.ROLE_SPEAKER,
                PageRequest.of(0, 5)
        );
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
                .reviews(mapSpeakerReviews(recentReviews))
                .build();
    }


    /**
     * 리스너 프로필 제작
     * 닉네임, 이미 등록했는지 확인
     * 프로필 저장 + 사용자 currentRole 수정
     */
    @Override
    public ProfileResponse createListenerProfile(Long userId, ListenerProfileRequest request) {
        User user = userService.findUserById(userId);
        ListenerProfile profile = createListenerProfileFromRequest(user, request);
        return saveProfileAndUpdateRole(profile, RoleType.ROLE_LISTENER);
    }

    /**
     * 스피커 프로필 제작
     * 닉네임, 이미 등록했는지 확인
     * 프로필 저장 + 사용자 currentRole 수정
     */
    @Override
    public ProfileResponse createSpeakerProfile(Long userId, SpeakerProfileRequest request) {
        User user = userService.findUserById(userId);
        SpeakerProfile profile = createSpeakerProfileFromRequest(user, request);

        return saveProfileAndUpdateRole(profile, RoleType.ROLE_SPEAKER);
    }

    /**
     * 역할 변경
     * 유효성 검사(targetRole 적절한지, 해당 정보 등록됐는지)
     * user currentRole 업데이트
     */
    @Override
    public ProfileStatusResponse switchRole(Long userId, RoleType targetRole) {
        User user = userService.findUserById(userId);
        validateRoleTransition(user.getCurrentRole(), targetRole);

        boolean hasListenerProfile = user.getListenerProfile() != null;
        boolean hasSpeakerProfile = user.getSpeakerProfile() != null;

        if (targetRole == RoleType.ROLE_LISTENER && !hasListenerProfile) {
            return ProfileStatusResponse.of("PROFILE_REQUIRED", "리스너 프로필 작성이 필요합니다", user.getCurrentRole(), hasListenerProfile, hasSpeakerProfile);
        }

        if (targetRole == RoleType.ROLE_SPEAKER && !hasSpeakerProfile) {
            return ProfileStatusResponse.of("PROFILE_REQUIRED", "스피커 프로필 작성이 필요합니다", user.getCurrentRole(), hasListenerProfile, hasSpeakerProfile);
        }

        user.updateRole(targetRole);
        return ProfileStatusResponse.of("SUCCESS", "역할이 변경되었습니다.", user.getCurrentRole(), hasListenerProfile, hasSpeakerProfile);
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
            profile.updateAvailableTime(request.getAvailableTimes());
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
    public ListenerProfileResponse updateListenerCertification(Long profileId, CertificationUpdateRequest request) {
        ListenerProfile profile = findListenerProfile(profileId);

        profile.updateCertificationDetails(request.getCertificationUrl(), request.getCareerDescription());
        return getListenerProfile(profile.getId());
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

    private User getCurrentUser() {
        return userService.findUserById(securityUtil.getCurrentUser().getId());
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
        if (listenerRepository.existsByNickname(nickname) || speakerRepository.existsByNickname(nickname)) {
            throw new CustomException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateProfileExistence(User user, RoleType roleType) {
        if (roleType == RoleType.ROLE_LISTENER && user.getListenerProfile() != null) {
            throw new CustomException(ProfileErrorCode.PROFILE_ALREADY_EXIST);
        }

        if (roleType == RoleType.ROLE_SPEAKER && user.getSpeakerProfile() != null) {
            throw new CustomException(ProfileErrorCode.PROFILE_ALREADY_EXIST);
        }
    }

    private <T> ProfileResponse saveProfileAndUpdateRole(T profile, RoleType roleType) {
        if (profile instanceof ListenerProfile listenerProfile) {
            listenerRepository.save(listenerProfile);
            listenerProfile.getUser().updateRole(roleType);
            return ProfileResponse.of(listenerProfile.getId(), listenerProfile.getNickname(), roleType, "리스너 프로필이 생성되었습니다.");
        } else if (profile instanceof SpeakerProfile speakerProfile) {
            speakerRepository.save(speakerProfile);
            speakerProfile.getUser().updateRole(roleType);
            return ProfileResponse.of(speakerProfile.getId(), speakerProfile.getNickname(), roleType, "스피커 프로필이 생성되었습니다");
        }

        throw new CustomException(ProfileErrorCode.INVALID_ROLE_TYPE);
    }

    private ListenerProfile createListenerProfileFromRequest(User user, ListenerProfileRequest request) {
        validateUniqueNickname(request.getNickname());
        validateProfileExistence(user, RoleType.ROLE_LISTENER);

        ListenerProfile profile = ListenerProfile.builder()
                .user(user)
                .nickname(request.getNickname())
                .profileImage(request.getProfileImage())
                .counselingStyle(request.getCounselingStyle())
                .availableTime(request.getAvailableTime())
                .build();

        request.getCounselingFields().forEach(profile::addCounselingField);
        return profile;
    }

    private SpeakerProfile createSpeakerProfileFromRequest(User user, SpeakerProfileRequest request) {
        validateUniqueNickname(request.getNickname());
        validateProfileExistence(user, RoleType.ROLE_SPEAKER);

        return SpeakerProfile.builder()
                .user(user)
                .nickname(request.getNickname())
                .profileImage(request.getProfileImage())
                .preferredCounselingStyle(request.getPreferredCounselingStyle())
                .build();
    }

    private ListenerProfile findListenerProfile(Long profileId) {
        return listenerRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private SpeakerProfile findSpeakerProfile(Long profileId) {
        return speakerRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ProfileErrorCode.PROFILE_NOT_FOUND));
    }

    private List<ReviewResponse> mapListenerReviews(List<Review> reviews) {
        return reviews.stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .content(review.getContent())
                        .rating(review.getRating())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ReviewResponse> mapSpeakerReviews(List<Review> reviews) {
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


}
