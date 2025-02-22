package com.mindmate.mindmate_server.profile.service;

import com.mindmate.mindmate_server.auth.util.SecurityUtil;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.*;
import com.mindmate.mindmate_server.user.dto.ListenerProfileRequest;
import com.mindmate.mindmate_server.user.dto.ProfileResponse;
import com.mindmate.mindmate_server.user.dto.ProfileStatusResponse;
import com.mindmate.mindmate_server.user.dto.SpeakerProfileRequest;
import com.mindmate.mindmate_server.user.repository.ListenerRepository;
import com.mindmate.mindmate_server.user.repository.SpeakerRepository;
import com.mindmate.mindmate_server.user.service.ProfileServiceImpl;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private UserService userService;
    @Mock private ListenerRepository listenerRepository;
    @Mock private SpeakerRepository speakerRepository;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = createDefaultUser();
        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userService.findUserById(any())).thenReturn(mockUser);
    }

    @Nested
    @DisplayName("리스너 프로필 생성 테스트")
    class CreateListenerProfileTest {
        @Test
        @DisplayName("리스너 프로필 생성 성공")
        void createListenerProfile_Success() {
            // given
            ListenerProfileRequest request = createListenerRequest();

            when(userService.findUserById(any())).thenReturn(mockUser);
            when(speakerRepository.existsByNickname(anyString())).thenReturn(false);
            when(listenerRepository.existsByNickname(anyString())).thenReturn(false);

            // when
            ProfileResponse response = profileService.createListenerProfile(request);

            // then
            assertNotNull(response);
            assertEquals(RoleType.ROLE_LISTENER, response.getRole());

            verify(listenerRepository).save(any(ListenerProfile.class));
        }

        @Test
        @DisplayName("중복 닉네임으로 인한 실패")
        void createListenerProfile_DuplicateNickname() {
            // given
            ListenerProfileRequest request = createListenerRequest();
            when(listenerRepository.existsByNickname(anyString())).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> profileService.createListenerProfile(request));
        }

        @Test
        @DisplayName("이미 프로필이 존재하는 경우 실패")
        void createListenerProfile_AlreadyExists() {
            // given
            ListenerProfile mockProfile = mock(ListenerProfile.class);
            mockUser.setListenerProfileForTest(mockProfile);
            ListenerProfileRequest request = createListenerRequest();

            // when & then
            assertThrows(CustomException.class, () -> profileService.createListenerProfile(request));
            assertNotNull(mockUser.getListenerProfile());
        }
    }

    @Nested
    @DisplayName("스피커 프로필 생성 테스트")
    class CreateSpeakerProfileTest {
        @Test
        @DisplayName("스피커 프로필 생성 성공")
        void createSpeakerProfile_Success() {
            // given
            SpeakerProfileRequest request = createSpeakerRequest();
            when(speakerRepository.existsByNickname(any())).thenReturn(false);
            when(listenerRepository.existsByNickname(any())).thenReturn(false);

            // when
            ProfileResponse response = profileService.createSpeakerProfile(request);

            // then
            assertNotNull(response);
            assertEquals(RoleType.ROLE_SPEAKER, response.getRole());
            verify(speakerRepository).save(any(SpeakerProfile.class));
//            verify(mockUser).updateRole(RoleType.ROLE_SPEAKER);
        }
    }

    @Nested
    @DisplayName("역할 전환 테스트")
    class SwitchRoleTest {
        @Test
        @DisplayName("리스너 역할 전환 성공")
        void switchRole_ToListener_Success() {
            // given
            ListenerProfile mockListenerProfile = mock(ListenerProfile.class);
            mockUser.setListenerProfileForTest(mockListenerProfile);;

            // when
            ProfileStatusResponse response = profileService.switchRole(RoleType.ROLE_LISTENER);

            // then
            assertEquals("SUCCESS", response.getStatus());
            assertEquals(RoleType.ROLE_LISTENER, response.getCurrentRole());
            assertTrue(response.isHasListenerProfile());
            assertFalse(response.isHasSpeakerProfile());
        }
    }

    private User createDefaultUser() {
        return User.builder()
                .email("test@example.com")
                .password("password")
                .role(RoleType.ROLE_USER)
                .build();
    }

    private ListenerProfileRequest createListenerRequest() {
        return ListenerProfileRequest.builder()
                .nickname("listenerNickname")
                .counselingStyle(CounselingStyle.ACTIVE_LISTENING)
                .availableTime(LocalDateTime.now())
                .counselingFields(Set.of(CounselingField.CAREER, CounselingField.ADDICTION))
                .build();
    }

    private SpeakerProfileRequest createSpeakerRequest() {
        return SpeakerProfileRequest.builder()
                .nickname("speakerNickname")
                .preferredCounselingStyle(CounselingStyle.EMPHATHETIC)
                .build();
    }

}
