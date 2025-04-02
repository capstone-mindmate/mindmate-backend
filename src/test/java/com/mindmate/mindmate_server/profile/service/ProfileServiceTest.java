package com.mindmate.mindmate_server.profile.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.*;
import com.mindmate.mindmate_server.user.dto.*;
import com.mindmate.mindmate_server.user.repository.ProfileRepository;
import com.mindmate.mindmate_server.user.service.ProfileServiceImpl;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceTest {
    @Mock private UserService userService;
    @Mock private ProfileRepository profileRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock
    private User mockUser;
    @Mock
    private Profile mockProfile;
    @Mock
    private ChatRoom mockChatRoom;
    private Review mockReview;
    private List<ReviewResponse> emptyReviewResponses;

    @BeforeEach
    void setUp() {
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getEmail()).thenReturn("test@ajou.ac.kr");

        when(mockProfile.getId()).thenReturn(1L);
        when(mockProfile.getUser()).thenReturn(mockUser);
        when(mockProfile.getNickname()).thenReturn("ajou");
        when(mockProfile.getProfileImage()).thenReturn("http://example.com/image.jpg");
        when(mockProfile.getDepartment()).thenReturn("소프트웨어학과");
        when(mockProfile.getEntranceTime()).thenReturn(2020);
        when(mockProfile.isGraduation()).thenReturn(false);
        when(mockProfile.getCounselingCount()).thenReturn(0);
        when(mockProfile.getAvgResponseTime()).thenReturn(0);


        mockReview = mock(Review.class);
        when(mockReview.getId()).thenReturn(1L);
        when(mockReview.getReviewer()).thenReturn(mockUser);
        when(mockReview.getReviewedProfile()).thenReturn(mockProfile);
        when(mockReview.getChatRoom()).thenReturn(mockChatRoom);
        when(mockReview.getRating()).thenReturn(5);
        when(mockReview.getComment()).thenReturn("아주 나이스한 상담이었습니다.");

        emptyReviewResponses = new ArrayList<>();
    }

    @Test
    @DisplayName("사용자 ID로 프로필 상세 조회 테스트")
    void getProfileDetail() {
        Long userId = 1L;
        List<Review> reviews = Collections.singletonList(mockReview);
        Double averageRating = 4.5;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));
        when(reviewRepository.findRecentReviewsByRevieweeId(any(Long.class), any(PageRequest.class)))
                .thenReturn(reviews);
        when(reviewRepository.calculateAverageRatingByRevieweeId(any(Long.class)))
                .thenReturn(Optional.of(averageRating));

        ProfileDetailResponse expectedResponse = ProfileDetailResponse.builder()
                .id(1L)
                .userId(1L)
                .nickname("ajou")
                .profileImage("http://example.com/image.jpg")
                .department("소프트웨어학과")
                .entranceTime(2020)
                .graduation(false)
                .totalCounselingCount(0)
                .avgResponseTime(0)
                .averageRating(4.5)
                .reviews(emptyReviewResponses)
                .build();

        // when
        ProfileDetailResponse response = profileService.getProfileDetail(userId);

        // then
        assertNotNull(response);
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(reviewRepository).findRecentReviewsByRevieweeId(any(Long.class), any(PageRequest.class));
        verify(reviewRepository).calculateAverageRatingByRevieweeId(any(Long.class));
    }

    @Test
    @DisplayName("사용자 ID로 프로필 조회 시 프로필이 없는 경우 예외 발생")
    void getProfileDetailWhenProfileNotFound() {
        // given
        Long userId = 1L;
        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.getProfileDetail(userId);
        });

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("프로필 ID로 프로필 상세 조회 테스트")
    void getProfileDetailById() {
        // given
        Long profileId = 1L;
        List<Review> reviews = Collections.singletonList(mockReview);
        Double averageRating = 4.5;

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(mockProfile));
        when(reviewRepository.findRecentReviewsByRevieweeId(eq(mockUser.getId()), any(PageRequest.class)))
                .thenReturn(reviews);
        when(reviewRepository.calculateAverageRatingByRevieweeId(mockUser.getId())).thenReturn(Optional.of(averageRating));

        // when
        ProfileDetailResponse response = profileService.getProfileDetailById(profileId);

        // then
        assertNotNull(response);
        verify(profileRepository).findById(profileId);
        verify(reviewRepository).findRecentReviewsByRevieweeId(eq(1L), any(PageRequest.class));
        verify(reviewRepository).calculateAverageRatingByRevieweeId(1L);
    }

    @Test
    @DisplayName("프로필 ID로 프로필 조회 시 프로필이 없는 경우 예외 발생")
    void getProfileDetailByIdWhenProfileNotFound() {
        // given
        Long profileId = 1L;
        when(profileRepository.findById(profileId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.getProfileDetailById(profileId);
        });

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("간소화된 프로필 조회 테스트")
    void getProfileSimple() {
        // given
        Long userId = 1L;
        Double averageRating = 4.5;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));
        when(reviewRepository.calculateAverageRatingByRevieweeId(userId)).thenReturn(Optional.of(averageRating));

        // when
        ProfileSimpleResponse response = profileService.getProfileSimple(userId);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockUser.getId(), response.getUserId());
        assertEquals(mockProfile.getNickname(), response.getNickname());
        assertEquals(mockProfile.getProfileImage(), response.getProfileImage());
        assertEquals(mockProfile.getCounselingCount(), response.getTotalCounselingCount());
        assertEquals(averageRating, response.getAverageRating());
    }

    @Test
    @DisplayName("프로필 생성 테스트")
    void createProfile() {
        // given
        Long userId = 1L;
        ProfileCreateRequest request = ProfileCreateRequest.builder()
                .profileImage("http://example.com/new_image.jpg")
                .nickname("ajou")
                .department("소프트웨어학과")
                .entranceTime(2020)
                .graduation(false)
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.existsByNickname(anyString())).thenReturn(false);

        Profile savedProfile = mock(Profile.class);
        when(savedProfile.getId()).thenReturn(1L);
        when(savedProfile.getNickname()).thenReturn("ajou");
        when(profileRepository.save(any(Profile.class))).thenReturn(savedProfile);

        // when
        ProfileResponse response = profileService.createProfile(userId, request);

        // then
        assertNotNull(response);

        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(profileRepository).existsByNickname(request.getNickname());
        verify(profileRepository).save(any(Profile.class));
        verify(mockUser).updateRole(RoleType.ROLE_PROFILE);
    }

    @Test
    @DisplayName("이미 존재하는 프로필 생성 시 예외 발생 테스트")
    void createProfileWhenAlreadyExists() {
        // given
        Long userId = 1L;
        ProfileCreateRequest request = ProfileCreateRequest.builder()
                .nickname("ajou")
                .profileImage("http://example.com/new_image.jpg")
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.createProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.PROFILE_ALREADY_EXIST, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미 존재하는 닉네임 생성 시 예외 발생 테스트")
    void createProfileWhenNicknameExists() {
        // given
        Long userId = 2L;
        ProfileCreateRequest request = ProfileCreateRequest.builder()
                .nickname("ajou")
                .profileImage("http://example.com/new_image.jpg")
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.existsByNickname("ajou")).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.createProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
    }

    @Test
    @DisplayName("프로필 업데이트 테스트 - 프로필 이미지 변경")
    void updateProfile() {
        // given
        Long userId = 1L;
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .profileImage("http://example.com/updated_image.jpg")
                .nickname("new ajou")
                .department("심리학과")
                .entranceTime(2020)
                .graduation(true)
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(mockProfile));
        when(profileRepository.existsByNickname("new ajou")).thenReturn(false);

        // when
        ProfileResponse response = profileService.updateProfile(userId, request);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockProfile.getNickname(), response.getNickname());
        assertEquals("프로필이 업데이트되었습니다.", response.getMessage());

        verify(profileRepository).findByUserId(any());
        verify(mockProfile).updateNickname(request.getNickname());
        verify(mockProfile).updateProfileImage(request.getProfileImage());
        verify(mockProfile).updateDepartment(request.getDepartment());
        verify(mockProfile).updateEntranceTime(request.getEntranceTime());
        verify(mockProfile).updateGraduation(request.getGraduation());
    }

    @Test
    @DisplayName("상담 횟수 증가 테스트")
    void incrementCounselingCount() {
        // given
        Long userId = 1L;
        int initialCount = mockProfile.getCounselingCount();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(mockProfile));

        // when
        profileService.incrementCounselingCount(userId);

        // then
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(mockProfile).incrementCounselingCount();
    }

    @Test
    @DisplayName("응답 시간 업데이트 테스트")
    void updateResponseTime() {
        // given
        Long userId = 1L;
        Integer responseTime = 120;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(any())).thenReturn(Optional.of(mockProfile));

        // when
        profileService.updateResponseTime(userId, responseTime);

        // then
        verify(mockProfile).updateResponseTime(responseTime);
    }


    @Test
    @DisplayName("프로필이 없는 경우 새 프로필 생성 테스트")
    void getOrCreateProfileWhenProfileNotExists() {
        // given
        Long userId = 1L;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        // when - 프로필 조회 또는 생성 내부 메서드 호출을 위해 다른 메서드를 사용
        profileService.incrementCounselingCount(userId);

        // then - save 메서드가 호출되었는지 확인
        verify(profileRepository).save(any(Profile.class));
    }
}