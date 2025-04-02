package com.mindmate.mindmate_server.profile.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.review.domain.Review;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private UserService userService;
    @Mock private ProfileRepository profileRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User mockUser;
    private Profile mockProfile;
    private Review mockReview;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@ajou.ac.kr")
                .password("password")
                .agreedToTerms(true)
                .role(RoleType.ROLE_USER)
                .build();

        ReflectionTestUtils.setField(mockUser, "id", 1L);


        mockProfile = Profile.builder()
                .user(mockUser)
                .profileImage("http://example.com/image.jpg")
                .nickname("ajou")
                .department("소프트웨어학과")
                .entranceTime(2020)
                .graduation(false)
                .build();

        mockReview = Review.builder()
                .reviewer(mockUser)
//                .reviewee(mockUser) // 우선 같은 걸로
//                .content("아주 나이스한 상담이었습니다.")
//                .rating(4.5)
                .build();
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

        // when
        ProfileDetailResponse response = profileService.getProfileDetail(userId);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockUser.getId(), response.getUserId());
        assertEquals(mockProfile.getNickname(), response.getNickname());
        assertEquals(mockProfile.getProfileImage(), response.getProfileImage());
        assertEquals(mockProfile.getDepartment(), response.getDepartment());
        assertEquals(mockProfile.getEntranceTime(), response.getEntranceTime());
        assertEquals(mockProfile.isGraduation(), response.isGraduation());
        assertEquals(mockProfile.getCounselingCount(), response.getTotalCounselingCount());
        assertEquals(mockProfile.getAvgResponseTime(), response.getAvgResponseTime());
        assertEquals(averageRating, response.getAverageRating());
        assertEquals(1, response.getReviews().size());

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
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockUser.getId(), response.getUserId());
        assertEquals(mockProfile.getNickname(), response.getNickname());
        assertEquals(mockProfile.getDepartment(), response.getDepartment());
        assertEquals(mockProfile.getEntranceTime(), response.getEntranceTime());
        assertEquals(mockProfile.isGraduation(), response.isGraduation());
        assertEquals(averageRating, response.getAverageRating());
        assertEquals(1, response.getReviews().size());
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
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        // when
        ProfileResponse response = profileService.createProfile(userId, request);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(request.getNickname(), response.getNickname());
        assertEquals("프로필이 생성되었습니다.", response.getMessage());

        verify(profileRepository).save(any(Profile.class));
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

        // when
        ProfileResponse response = profileService.updateProfile(userId, request);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockProfile.getNickname(), response.getNickname());
        assertEquals("프로필이 업데이트되었습니다.", response.getMessage());

        verify(profileRepository).findByUserId(any());
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
        assertEquals(initialCount + 1, mockProfile.getCounselingCount());
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
        assertEquals(1, mockProfile.getResponseTimeCount());
        assertEquals(responseTime, mockProfile.getTotalResponseTime());
        assertEquals(responseTime, mockProfile.getAvgResponseTime());
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
