package com.mindmate.mindmate_server.profile.service;

import com.mindmate.mindmate_server.auth.util.SecurityUtil;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.*;
import com.mindmate.mindmate_server.user.dto.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Nested
    @DisplayName("리스너 프로필 생성 테스트")
    class CreateListenerProfileTest {
        private User mockUser;

        @BeforeEach
        void setup() {
            mockUser = createDefaultUser();
//            when(securityUtil.getCurrentUser()).thenReturn(mockUser);
            when(userService.findUserById(any())).thenReturn(mockUser);
        }
        @Test
        @DisplayName("리스너 프로필 생성 성공")
        void createListenerProfile_Success() {
            // given
            ListenerProfileRequest request = createListenerRequest();

            when(userService.findUserById(any())).thenReturn(mockUser);
            when(speakerRepository.existsByNickname(anyString())).thenReturn(false);
            when(listenerRepository.existsByNickname(anyString())).thenReturn(false);

            // when
            ProfileResponse response = profileService.createListenerProfile(mockUser.getId(), request);

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
            assertThrows(CustomException.class, () -> profileService.createListenerProfile(mockUser.getId(), request));
        }

        @Test
        @DisplayName("이미 프로필이 존재하는 경우 실패")
        void createListenerProfile_AlreadyExists() {
            // given
            ListenerProfile mockProfile = mock(ListenerProfile.class);
            mockUser.setListenerProfileForTest(mockProfile);
            ListenerProfileRequest request = createListenerRequest();

            // when & then
            assertThrows(CustomException.class, () -> profileService.createListenerProfile(mockUser.getId(), request));
            assertNotNull(mockUser.getListenerProfile());
        }
    }

    @Nested
    @DisplayName("스피커 프로필 생성 테스트")
    class CreateSpeakerProfileTest {
        private User mockUser;

        @BeforeEach
        void setup() {
            mockUser = createDefaultUser();
//            when(securityUtil.getCurrentUser()).thenReturn(mockUser);
            when(userService.findUserById(any())).thenReturn(mockUser);
        }

        @Test
        @DisplayName("스피커 프로필 생성 성공")
        void createSpeakerProfile_Success() {
            // given
            SpeakerProfileRequest request = createSpeakerRequest();
            when(speakerRepository.existsByNickname(any())).thenReturn(false);
            when(listenerRepository.existsByNickname(any())).thenReturn(false);

            // when
            ProfileResponse response = profileService.createSpeakerProfile(mockUser.getId(), request);

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
        private User mockUser;

        @BeforeEach
        void setup() {
            mockUser = createDefaultUser();
//            when(securityUtil.getCurrentUser()).thenReturn(mockUser);
            when(userService.findUserById(any())).thenReturn(mockUser);
        }
        @Test
        @DisplayName("리스너 역할 전환 성공")
        void switchRole_ToListener_Success() {
            // given
            ListenerProfile mockListenerProfile = mock(ListenerProfile.class);
            mockUser.setListenerProfileForTest(mockListenerProfile);;

            // when
            ProfileStatusResponse response = profileService.switchRole(mockUser.getId(), RoleType.ROLE_LISTENER);

            // then
            assertEquals("SUCCESS", response.getStatus());
            assertEquals(RoleType.ROLE_LISTENER, response.getCurrentRole());
            assertTrue(response.isHasListenerProfile());
            assertFalse(response.isHasSpeakerProfile());
        }

        @Test
        @DisplayName("스피커 역할 전환 성공")
        void switchRole_ToSpeaker_Success() {
            // given
            SpeakerProfile mockSpeakerProfile = mock(SpeakerProfile.class);
            mockUser.setSpeakerProfileForTest(mockSpeakerProfile);;

            // when
            ProfileStatusResponse response = profileService.switchRole(mockUser.getId(), RoleType.ROLE_SPEAKER);

            // then
            assertEquals("SUCCESS", response.getStatus());
            assertEquals(RoleType.ROLE_SPEAKER, response.getCurrentRole());
            assertFalse(response.isHasListenerProfile());
            assertTrue(response.isHasSpeakerProfile());
        }

        @Test
        @DisplayName("리스너 프로필 없이 역할 전환 시도 실패")
        void switchRole_ToListenerWithoutProfile_Fail() {
            // when
            ProfileStatusResponse response = profileService.switchRole(mockUser.getId(), RoleType.ROLE_LISTENER);

            // then
            assertEquals("PROFILE_REQUIRED", response.getStatus());
            assertEquals(RoleType.ROLE_USER, response.getCurrentRole());
            assertFalse(response.isHasListenerProfile());
        }

        @Test
        @DisplayName("스피커 프로필 없이 역할 전환 시도 실패")
        void switchRole_ToSpeakerWithoutProfile_Fail() {
            // when
            ProfileStatusResponse response = profileService.switchRole(mockUser.getId(), RoleType.ROLE_SPEAKER);

            // then
            assertEquals("PROFILE_REQUIRED", response.getStatus());
            assertEquals(RoleType.ROLE_USER, response.getCurrentRole());
            assertFalse(response.isHasSpeakerProfile());
        }

        @Test
        @DisplayName("동일한 역할로 전환 시도 실패")
        void switchRole_SameRole_Fail() {
            // given
            mockUser.updateRole(RoleType.ROLE_LISTENER);
            ListenerProfile mockListenerProfile = mock(ListenerProfile.class);
            mockUser.setListenerProfileForTest(mockListenerProfile);

            // when & then
            assertThrows(CustomException.class, () -> profileService.switchRole(mockUser.getId(), RoleType.ROLE_LISTENER));
        }

        @Test
        @DisplayName("잘못된 역할로 전환 시도 실패")
        void switchRole_InvalidRole_Fail() {
            // when & then
            assertThrows(CustomException.class, () -> profileService.switchRole(mockUser.getId(), RoleType.ROLE_ADMIN));
        }

    }

    @Nested
    @DisplayName("리스너 프로필 수정 테스트")
    class UpdateListenerProfileTest {
        @Test
        @DisplayName("리스너 프로필 수정 성공")
        void updateListenerProfile_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            when(profile.getId()).thenReturn(profileId);

            ListenerProfileUpdateRequest request = ListenerProfileUpdateRequest.builder()
                    .nickname("newNickname")
                    .profileImage("newImage.jpg")
                    .counselingStyle(CounselingStyle.ACTIVE_LISTENING)
                    .counselingFields(List.of(CounselingField.CAREER))
                    .availableTimes(LocalDateTime.now())
                    .build();

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(listenerRepository.existsByNickname(request.getNickname())).thenReturn(false);
            when(speakerRepository.existsByNickname(request.getNickname())).thenReturn(false);

            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            User mockUser = mock(User.class);
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getNickname()).thenReturn(request.getNickname());
            when(profile.getProfileImage()).thenReturn(request.getProfileImage());
            when(profile.getCounselingStyle()).thenReturn(request.getCounselingStyle());
            when(profile.getCounselingCount()).thenReturn(0);
            when(profile.getAvgResponseTime()).thenReturn(0);
            when(profile.getAvailableTime()).thenReturn(request.getAvailableTimes());
            when(profile.getCounselingFields()).thenReturn(new HashSet<>());

            // when
            ListenerProfileResponse response = profileService.updateListenerProfile(profileId, request);

            // then
            assertNotNull(response);
            assertEquals(profileId, response.getId());
            assertEquals(request.getNickname(), response.getNickname());

            verify(profile).updateNickname(request.getNickname());
            verify(profile).updateProfileImage(request.getProfileImage());
            verify(profile).updateCounselingStyle(request.getCounselingStyle());
            verify(profile).updateCounselingFields(request.getCounselingFields());
            verify(profile).updateAvailableTime(request.getAvailableTimes());
        }

        @Test
        @DisplayName("존재하지 않는 프로필 수정 시도 시 예외 발생")
        void updateListenerProfile_NotFound() {
            // given
            Long profileId = 999L;
            ListenerProfileUpdateRequest request = ListenerProfileUpdateRequest.builder()
                    .nickname("newNickname")
                    .build();

            when(listenerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateListenerProfile(profileId, request));
        }

        @Test
        @DisplayName("중복된 닉네임으로 수정 시도 시 예외 발생")
        void updateListenerProfile_DuplicateNickname() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            ListenerProfileUpdateRequest request = ListenerProfileUpdateRequest.builder()
                    .nickname("existingNickname")
                    .build();

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(listenerRepository.existsByNickname(request.getNickname())).thenReturn(true);

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateListenerProfile(profileId, request));
        }

        @Test
        @DisplayName("닉네임만 수정 성공")
        void updateListenerProfile_NicknameOnly_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            when(profile.getId()).thenReturn(profileId);
            User mockUser = mock(User.class);

            ListenerProfileUpdateRequest request = ListenerProfileUpdateRequest.builder()
                    .nickname("newNickname")
                    .build();  // 다른 필드는 null

            // Repository mocking
            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(listenerRepository.existsByNickname(request.getNickname())).thenReturn(false);
            when(speakerRepository.existsByNickname(request.getNickname())).thenReturn(false);

            // Profile mocking for getListenerProfile
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getNickname()).thenReturn(request.getNickname());
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            profileService.updateListenerProfile(profileId, request);

            // then
            verify(profile).updateNickname(request.getNickname());
            verify(profile, never()).updateProfileImage(any());
            verify(profile, never()).updateCounselingStyle(any());
            verify(profile, never()).updateCounselingFields(any());
            verify(profile, never()).updateAvailableTime(any());
        }

        @Test
        @DisplayName("프로필 이미지만 수정 성공")
        void updateListenerProfile_ImageOnly_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            when(profile.getId()).thenReturn(profileId);
            User mockUser = mock(User.class);

            ListenerProfileUpdateRequest request = ListenerProfileUpdateRequest.builder()
                    .profileImage("newImage.jpg")
                    .build();

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profile.getUser()).thenReturn(mockUser);
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            profileService.updateListenerProfile(profileId, request);

            // then
            verify(profile).updateProfileImage(request.getProfileImage());
            verify(profile, never()).updateNickname(any());
            verify(profile, never()).updateCounselingStyle(any());
            verify(profile, never()).updateCounselingFields(any());
            verify(profile, never()).updateAvailableTime(any());
        }
    }

    @Nested
    @DisplayName("스피커 프로필 수정 테스트")
    class UpdateSpeakerProfileTest {
        @Test
        @DisplayName("스피커 프로필 수정 성공")
        void updateSpeakerProfile_Success() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            when(profile.getId()).thenReturn(profileId);

            SpeakerProfileUpdateRequest request = SpeakerProfileUpdateRequest.builder()
                    .nickname("newNickname")
                    .profileImage("newImage.jpg")
                    .counselingStyle(CounselingStyle.EMPHATHETIC)
                    .build();

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(listenerRepository.existsByNickname(request.getNickname())).thenReturn(false);
            when(speakerRepository.existsByNickname(request.getNickname())).thenReturn(false);

            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            User mockUser = mock(User.class);
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getNickname()).thenReturn(request.getNickname());
            when(profile.getProfileImage()).thenReturn(request.getProfileImage());
            when(profile.getPreferredCounselingStyle()).thenReturn(request.getCounselingStyle());
            when(profile.getCounselingCount()).thenReturn(0);

            // when
            SpeakerProfileResponse response = profileService.updateSpeakerProfile(profileId, request);

            // then
            assertNotNull(response);
            assertEquals(profileId, response.getId());
            assertEquals(request.getNickname(), response.getNickname());

            verify(profile).updateNickname(request.getNickname());
            verify(profile).updateProfileImage(request.getProfileImage());
            verify(profile).updateCounselingStyle(request.getCounselingStyle());
        }

        @Test
        @DisplayName("존재하지 않는 프로필 수정 시도 시 예외 발생")
        void updateSpeakerProfile_NotFound() {
            // given
            Long profileId = 999L;
            SpeakerProfileUpdateRequest request = SpeakerProfileUpdateRequest.builder()
                    .nickname("newNickname")
                    .build();

            when(speakerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateSpeakerProfile(profileId, request));
        }

        @Test
        @DisplayName("중복된 닉네임으로 수정 시도 시 예외 발생")
        void updateSpeakerProfile_DuplicateNickname() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            SpeakerProfileUpdateRequest request = SpeakerProfileUpdateRequest.builder()
                    .nickname("existingNickname")
                    .build();

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(speakerRepository.existsByNickname(request.getNickname())).thenReturn(true);

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateSpeakerProfile(profileId, request));
        }

        @Test
        @DisplayName("닉네임만 수정 성공")
        void updateSpeakerProfile_NicknameOnly_Success() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            when(profile.getId()).thenReturn(profileId);
            User mockUser = mock(User.class);

            SpeakerProfileUpdateRequest request = SpeakerProfileUpdateRequest.builder()
                    .nickname("newNickname")
                    .build();

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(listenerRepository.existsByNickname(request.getNickname())).thenReturn(false);
            when(speakerRepository.existsByNickname(request.getNickname())).thenReturn(false);

            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getNickname()).thenReturn(request.getNickname());
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            profileService.updateSpeakerProfile(profileId, request);

            // then
            verify(profile).updateNickname(request.getNickname());
            verify(profile, never()).updateProfileImage(any());
            verify(profile, never()).updateCounselingStyle(any());
        }

        @Test
        @DisplayName("프로필 이미지와 상담 스타일만 수정 성공")
        void updateSpeakerProfile_ImageAndStyle_Success() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            when(profile.getId()).thenReturn(profileId);
            User mockUser = mock(User.class);

            SpeakerProfileUpdateRequest request = SpeakerProfileUpdateRequest.builder()
                    .profileImage("newImage.jpg")
                    .counselingStyle(CounselingStyle.EMPHATHETIC)
                    .build();  // nickname은 null

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profile.getUser()).thenReturn(mockUser);
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            profileService.updateSpeakerProfile(profileId, request);

            // then
            verify(profile).updateProfileImage(request.getProfileImage());
            verify(profile).updateCounselingStyle(request.getCounselingStyle());
            verify(profile, never()).updateNickname(any());
        }
    }

    @Nested
    @DisplayName("리스너 자격 인증 수정 테스트")
    class UpdateListenerCertificationTest {
        @Test
        @DisplayName("리스너 자격 인증 수정 성공")
        void updateListenerCertification_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            when(profile.getId()).thenReturn(profileId);

            CertificationUpdateRequest request = CertificationUpdateRequest.builder()
                    .certificationUrl("http://new_certification.com")
                    .careerDescription("설명 수정이용")
                    .build();

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));

            User mockUser = mock(User.class);
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getNickname()).thenReturn("listener");
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            ListenerProfileResponse response = profileService.updateListenerCertification(profileId, request);

            // then
            assertNotNull(response);
            verify(profile).updateCertificationDetails(
                    request.getCertificationUrl(),
                    request.getCareerDescription()
            );
        }

        @Test
        @DisplayName("존재하지 않는 프로필의 자격 인증 수정 시도 시 예외 발생")
        void updateListenerCertification_NotFound() {
            // given
            Long profileId = 999L;
            CertificationUpdateRequest request = CertificationUpdateRequest.builder()
                    .certificationUrl("http://new_certification.com")
                    .careerDescription("설명이요")
                    .build();

            when(listenerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateListenerCertification(profileId, request));
        }
    }

    @Nested
    @DisplayName("프로필 업데이트 관련 테스트")
    class ProfileUpdateTest {
        @Test
        @DisplayName("리스너 평균 평점 업데이트 성공")
        void updateAverageRating_Listener_Success() {
            // given
            Long profileId = 1L;
            Float newRating = 4.5f;
            ListenerProfile profile = mock(ListenerProfile.class);

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(reviewRepository.countReviewsByRevieweeId(profileId)).thenReturn(10L);

            // when
            profileService.updateAverageRating(profileId, RoleType.ROLE_LISTENER, newRating);

            // then
            verify(profile).updateAverageRating(newRating, 10L);
        }

        @Test
        @DisplayName("스피커 평균 평점 업데이트 성공")
        void updateAverageRating_Speaker_Success() {
            // given
            Long profileId = 1L;
            Float newRating = 4.5f;
            SpeakerProfile profile = mock(SpeakerProfile.class);

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(reviewRepository.countReviewsByRevieweeId(profileId)).thenReturn(10L);

            // when
            profileService.updateAverageRating(profileId, RoleType.ROLE_SPEAKER, newRating);

            // then
            verify(profile).updateAverageRating(newRating, 10L);
        }

        @Test
        @DisplayName("리스너 응답 시간 업데이트 성공")
        void updateResponseTime_Success() {
            // given
            Long profileId = 1L;
            Integer responseTime = 30;
            ListenerProfile profile = mock(ListenerProfile.class);

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));

            // when
            profileService.updateResponseTime(profileId, responseTime);

            // then
            verify(profile).updateAverageResponseTime(responseTime);
        }

        @Test
        @DisplayName("리스너 상담 횟수 증가 성공")
        void updateCounselingCount_Listener_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));

            // when
            profileService.updateCounselingCount(profileId, RoleType.ROLE_LISTENER);

            // then
            verify(profile).incrementCounselingCount();
        }

        @Test
        @DisplayName("스피커 상담 횟수 증가 성공")
        void updateCounselingCount_Speaker_Success() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));

            // when
            profileService.updateCounselingCount(profileId, RoleType.ROLE_SPEAKER);

            // then
            verify(profile).incrementCounselingCount();
        }

        @Test
        @DisplayName("존재하지 않는 리스너 상담 횟수 증가 시도 시 예외 발생")
        void updateCounselingCount_ListenerNotFound() {
            // given
            Long profileId = 999L;
            when(listenerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateCounselingCount(profileId, RoleType.ROLE_LISTENER));
        }

        @Test
        @DisplayName("존재하지 않는 스피커 상담 횟수 증가 시도 시 예외 발생")
        void updateCounselingCount_SpeakerNotFound() {
            // given
            Long profileId = 999L;
            when(speakerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.updateCounselingCount(profileId, RoleType.ROLE_SPEAKER));
        }
    }

    @Nested
    @DisplayName("프로필 조회 테스트")
    class GetProfileTest {
        @Test
        @DisplayName("리스너 프로필 조회 성공")
        void getListenerProfile_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            User mockUser = mock(User.class);

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getId()).thenReturn(profileId);
            when(profile.getNickname()).thenReturn("listener");
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            ListenerProfileResponse response = profileService.getListenerProfile(profileId);

            // then
            assertNotNull(response);
            assertEquals(profileId, response.getId());
            assertEquals("listener", response.getNickname());
        }

        @Test
        @DisplayName("스피커 프로필 조회 성공")
        void getSpeakerProfile_Success() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            User mockUser = mock(User.class);

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getId()).thenReturn(profileId);
            when(profile.getNickname()).thenReturn("speaker");
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of());
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            SpeakerProfileResponse response = profileService.getSpeakerProfile(profileId);

            // then
            assertNotNull(response);
            assertEquals(profileId, response.getId());
            assertEquals("speaker", response.getNickname());
        }

        @Test
        @DisplayName("존재하지 않는 리스너 프로필 조회 시 예외 발생")
        void getListenerProfile_NotFound() {
            // given
            Long profileId = 999L;
            when(listenerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.getListenerProfile(profileId));
        }

        @Test
        @DisplayName("존재하지 않는 스피커 프로필 조회 시 예외 발생")
        void getSpeakerProfile_NotFound() {
            // given
            Long profileId = 999L;
            when(speakerRepository.findById(profileId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class,
                    () -> profileService.getSpeakerProfile(profileId));
        }

        @Test
        @DisplayName("리뷰가 있는 리스너 프로필 조회 성공")
        void getListenerProfile_WithReviews_Success() {
            // given
            Long profileId = 1L;
            ListenerProfile profile = mock(ListenerProfile.class);
            User mockUser = mock(User.class);

            Review mockReview = mock(Review.class);
            when(mockReview.getId()).thenReturn(1L);
            when(mockReview.getContent()).thenReturn("좋네요. 또 올게용!");
            when(mockReview.getRating()).thenReturn(5);
            when(mockReview.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(listenerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getId()).thenReturn(profileId);
            when(profile.getNickname()).thenReturn("listener");
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of(mockReview));
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            ListenerProfileResponse response = profileService.getListenerProfile(profileId);

            // then
            assertNotNull(response);
            assertEquals(profileId, response.getId());
            assertEquals("listener", response.getNickname());
            assertFalse(response.getReviews().isEmpty());
            assertEquals(1L, response.getReviews().get(0).getId());
            assertEquals("좋네요. 또 올게용!", response.getReviews().get(0).getContent());
            assertEquals(5, response.getReviews().get(0).getRating());
        }

        @Test
        @DisplayName("리뷰가 있는 스피커 프로필 조회 성공")
        void getSpeakerProfile_WithReviews_Success() {
            // given
            Long profileId = 1L;
            SpeakerProfile profile = mock(SpeakerProfile.class);
            User mockUser = mock(User.class);

            Review mockReview = mock(Review.class);
            when(mockReview.getId()).thenReturn(1L);
            when(mockReview.getContent()).thenReturn("좋아요");
            when(mockReview.getRating()).thenReturn(5);
            when(mockReview.getReply()).thenReturn("감삼당");
            when(mockReview.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(speakerRepository.findById(profileId)).thenReturn(Optional.of(profile));
            when(profile.getUser()).thenReturn(mockUser);
            when(profile.getId()).thenReturn(profileId);
            when(profile.getNickname()).thenReturn("speaker");
            when(reviewRepository.findRecentReviewsByUserIdAndRole(any(), any(), any()))
                    .thenReturn(List.of(mockReview));
            when(reviewRepository.calculateAverageRatingByRevieweeId(any()))
                    .thenReturn(Optional.of(4.5));

            // when
            SpeakerProfileResponse response = profileService.getSpeakerProfile(profileId);

            // then
            assertNotNull(response);
            assertEquals(profileId, response.getId());
            assertEquals("speaker", response.getNickname());
            assertFalse(response.getReviews().isEmpty());
            assertEquals(1L, response.getReviews().get(0).getId());
            assertEquals("좋아요", response.getReviews().get(0).getContent());
            assertEquals(5, response.getReviews().get(0).getRating());
            assertEquals("감삼당", response.getReviews().get(0).getReply());
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
