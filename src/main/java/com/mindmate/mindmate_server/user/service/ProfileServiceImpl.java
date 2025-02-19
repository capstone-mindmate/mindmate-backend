package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final UserService userService;
    private final ListenerRepository listenerRepository;
    private final SpeakerRepository speakerRepository;

    @Override
    public ProfileResponse createListenerProfile(ListenerProfileRequest request) {
        User user = SecurityUtil.getCurrentUser();
        validateUniqueNickname(request.getNickname());

        ListenerProfile profile = ListenerProfile.builder()
                .user(user)
                .nickname(request.getNickname())
                .counselingStyle(request.getCounselingStyle())
                .availableTime(request.getAvailableTime())
                .build();

        request.getCounselingFields().forEach(profile::addCounselingField);

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
        User user = SecurityUtil.getCurrentUser();
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
    public ProfileStatusResponse switchRole(RoleType targetRole) {
        User user = SecurityUtil.getCurrentUser();

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

    private void validateUniqueNickname(String nickname) {
        if (listenerRepository.existsByNickname(nickname) || speakerRepository.existsByNickname(nickname)) {
            throw new CustomException(ProfileErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
