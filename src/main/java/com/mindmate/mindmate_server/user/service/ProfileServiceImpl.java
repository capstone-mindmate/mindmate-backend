package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.util.SecurityUtil;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.ListenerProfileRequest;
import com.mindmate.mindmate_server.user.dto.ProfileResponse;
import com.mindmate.mindmate_server.user.dto.ProfileStatusResponse;
import com.mindmate.mindmate_server.user.dto.SpeakerProfileRequest;
import com.mindmate.mindmate_server.user.repository.ListenerRepository;
import com.mindmate.mindmate_server.user.repository.SpeakerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserService userService;
    private final ListenerRepository listenerRepository;
    private final SpeakerRepository speakerRepository;
    private final SecurityUtil securityUtil;

    /**
     * 리스너 프로필 제작
     * 닉네임, 이미 등록했는지 확인
     * 프로필 저장 + 사용자 currentRole 수정
     */
    @Override
    public ProfileResponse createListenerProfile(ListenerProfileRequest request) {
        User user = getCurrentUser();
        ListenerProfile profile = createListenerProfileFromRequest(user, request);
        return saveProfileAndUpdateRole(profile, RoleType.ROLE_LISTENER);
    }

    /**
     * 스피커 프로필 제작
     * 닉네임, 이미 등록했는지 확인
     * 프로필 저장 + 사용자 currentRole 수정
     */
    @Override
    public ProfileResponse createSpeakerProfile(SpeakerProfileRequest request) {
        User user = getCurrentUser();
        SpeakerProfile profile = createSpeakerProfileFromRequest(user, request);

        return saveProfileAndUpdateRole(profile, RoleType.ROLE_SPEAKER);
    }

    /**
     * 역할 변경
     * 유효성 검사(targetRole 적절한지, 해당 정보 등록됐는지)
     * user currentRole 업데이트
     */
    @Override
    public ProfileStatusResponse switchRole(RoleType targetRole) {
        User user = getCurrentUser();
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

}
