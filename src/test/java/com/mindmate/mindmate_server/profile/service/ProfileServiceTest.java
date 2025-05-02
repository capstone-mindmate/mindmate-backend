package com.mindmate.mindmate_server.profile.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.review.service.ReviewDataService;
import com.mindmate.mindmate_server.review.service.ReviewService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceTest {
    @Mock private UserService userService;
    @Mock private ProfileRepository profileRepository;
    @Mock private ReviewDataService reviewService;
    @Mock private MatchingService matchingService;
    @Mock private PointService pointService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock private User mockUser;
    @Mock private Profile mockProfile;
    @Mock private ChatRoom mockChatRoom;
    private Review mockReview;
    private List<ReviewResponse> mockReviewResponses;
    private Map<String, Integer> mockTagCounts;
    private Map<String, Integer> mockCategoryCounts;

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
        when(mockProfile.getCreatedAt()).thenReturn(LocalDateTime.now());

        mockReview = mock(Review.class);
        when(mockReview.getId()).thenReturn(1L);
        when(mockReview.getReviewer()).thenReturn(mockUser);
        when(mockReview.getReviewedProfile()).thenReturn(mockProfile);
        when(mockReview.getChatRoom()).thenReturn(mockChatRoom);
        when(mockReview.getRating()).thenReturn(5);
        when(mockReview.getComment()).thenReturn("아주 나이스한 상담이었습니다.");

        ReviewResponse reviewResponse = mock(ReviewResponse.class);
        mockReviewResponses = new ArrayList<>();
        mockReviewResponses.add(reviewResponse);

        mockTagCounts = new HashMap<>();
        mockTagCounts.put("공감", 3);
        mockTagCounts.put("친절", 2);

        mockCategoryCounts = new HashMap<>();
        mockCategoryCounts.put("학업", 5);
        mockCategoryCounts.put("진로", 3);

        when(reviewService.getRecentReviewsByUserId(anyLong(), anyInt())).thenReturn(mockReviewResponses);
        when(reviewService.getAverageRatingByUserId(anyLong())).thenReturn(4.5);
        when(reviewService.getTagCountsByProfileId(anyLong())).thenReturn(mockTagCounts);
        when(matchingService.getCategoryCountsByUserId(anyLong())).thenReturn(mockCategoryCounts);
        when(pointService.getCurrentBalance(anyLong())).thenReturn(100);
    }

    @Test
    @DisplayName("사용자 ID로 프로필 상세 조회 테스트")
    void getProfileDetail() {
        // given
        Long userId = 1L;

        when(profileRepository.findWithUserByUserId(userId)).thenReturn(Optional.of(mockProfile));
        when(mockProfile.getUser()).thenReturn(mockUser);

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
        assertEquals(4.5, response.getAverageRating());
        assertEquals(mockTagCounts, response.getTagCounts());
        assertEquals(mockCategoryCounts, response.getCategoryCounts());
        assertEquals(100, response.getPoints());
        assertEquals(mockReviewResponses, response.getReviews());

        verify(reviewService).getRecentReviewsByUserId(eq(userId), eq(5));
        verify(reviewService).getAverageRatingByUserId(userId);
        verify(reviewService).getTagCountsByProfileId(mockProfile.getId());
        verify(matchingService).getCategoryCountsByUserId(userId);
        verify(pointService).getCurrentBalance(userId);
    }

    @Test
    @DisplayName("사용자 ID로 프로필 조회 시 프로필이 없는 경우 예외 발생")
    void getProfileDetailWhenProfileNotFound() {
        // given
        Long userId = 1L;
        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findWithUserByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.getProfileDetail(userId);
        });

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
        verify(profileRepository).findWithUserByUserId(userId);
    }

    @Test
    @DisplayName("프로필 ID로 프로필 상세 조회 테스트")
    void getProfileDetailById() {
        // given
        Long profileId = 1L;

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(mockProfile));

        // when
        ProfileDetailResponse response = profileService.getProfileDetailById(profileId);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockUser.getId(), response.getUserId());
        verify(profileRepository).findById(profileId);
        verify(reviewService).getRecentReviewsByUserId(eq(mockUser.getId()), eq(5));
        verify(reviewService).getAverageRatingByUserId(mockUser.getId());
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
        verify(profileRepository).findById(profileId);
    }

    @Test
    @DisplayName("간소화된 프로필 조회 테스트")
    void getProfileSimple() {
        // given
        Long userId = 1L;
        Double averageRating = 4.5;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUser(mockUser)).thenReturn(Optional.of(mockProfile));
        when(reviewService.getAverageRatingByUserId(userId)).thenReturn(averageRating);

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

        verify(userService).findUserById(userId);
        verify(profileRepository).findByUser(mockUser);
        verify(reviewService).getAverageRatingByUserId(userId);
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
        assertEquals(savedProfile.getNickname(), response.getNickname());
        assertEquals("프로필이 생성되었습니다.", response.getMessage());

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
                .department("소프트웨어학과")
                .entranceTime(2020)
                .graduation(false)
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.createProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.PROFILE_ALREADY_EXIST, exception.getErrorCode());
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("이미 존재하는 닉네임 생성 시 예외 발생 테스트")
    void createProfileWhenNicknameExists() {
        // given
        Long userId = 2L;
        ProfileCreateRequest request = ProfileCreateRequest.builder()
                .nickname("ajou")
                .profileImage("http://example.com/new_image.jpg")
                .department("소프트웨어학과")
                .entranceTime(2020)
                .graduation(false)
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.existsByNickname("ajou")).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.createProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(profileRepository).existsByNickname("ajou");
    }

    @Test
    @DisplayName("유효하지 않은 입학년도로 프로필 생성 시 예외 발생 테스트")
    void createProfileWithInvalidEntranceTime() {
        // given
        Long userId = 1L;
        int currentYear = LocalDate.now().getYear();
        ProfileCreateRequest request = ProfileCreateRequest.builder()
                .nickname("ajou")
                .profileImage("http://example.com/new_image.jpg")
                .department("소프트웨어학과")
                .entranceTime(currentYear + 2)
                .graduation(false)
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.existsByNickname(anyString())).thenReturn(false);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.createProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.INVALID_ENTRANCE_TIME, exception.getErrorCode());
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("프로필 업데이트 테스트 - 모든 필드 변경")
    void updateProfile() {
        // given
        Long userId = 1L;
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .profileImage("http://example.com/updated_image.jpg")
                .nickname("new_ajou")
                .department("심리학과")
                .entranceTime(2021)
                .graduation(true)
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.existsByNickname("new_ajou")).thenReturn(false);

        // when
        ProfileResponse response = profileService.updateProfile(userId, request);

        // then
        assertNotNull(response);
        assertEquals(mockProfile.getId(), response.getId());
        assertEquals(mockProfile.getNickname(), response.getNickname());
        assertEquals("프로필이 업데이트되었습니다.", response.getMessage());

        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(mockProfile).updateNickname(request.getNickname());
        verify(mockProfile).updateProfileImage(request.getProfileImage());
        verify(mockProfile).updateDepartment(request.getDepartment());
        verify(mockProfile).updateEntranceTime(request.getEntranceTime());
        verify(mockProfile).updateGraduation(request.getGraduation());
    }

    @Test
    @DisplayName("프로필 업데이트 테스트 - 닉네임만 변경")
    void updateProfileOnlyNickname() {
        // given
        Long userId = 1L;
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .nickname("new_ajou")
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.existsByNickname("new_ajou")).thenReturn(false);

        // when
        ProfileResponse response = profileService.updateProfile(userId, request);

        // then
        assertNotNull(response);
        verify(mockProfile).updateNickname("new_ajou");
        verify(mockProfile, never()).updateProfileImage(any());
        verify(mockProfile, never()).updateDepartment(any());
        verify(mockProfile, never()).updateEntranceTime(any());
        verify(mockProfile, never()).updateGraduation(any());
    }

    @Test
    @DisplayName("프로필 업데이트 테스트 - 중복 닉네임 예외")
    void updateProfileWithDuplicateNickname() {
        // given
        Long userId = 1L;
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .nickname("existing_nickname")
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.existsByNickname("existing_nickname")).thenReturn(true);
        when(mockProfile.getNickname()).thenReturn("ajou"); // 현재 닉네임은 다름

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.updateProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.DUPLICATE_NICKNAME, exception.getErrorCode());
    }

    @Test
    @DisplayName("프로필 업데이트 테스트 - 유효하지 않은 입학년도")
    void updateProfileWithInvalidEntranceTime() {
        // given
        Long userId = 1L;
        int currentYear = LocalDate.now().getYear();
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .entranceTime(currentYear + 2) // 현재 년도 + 2는 유효하지 않음
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            profileService.updateProfile(userId, request);
        });

        assertEquals(ProfileErrorCode.INVALID_ENTRANCE_TIME, exception.getErrorCode());
    }

    @Test
    @DisplayName("존재하지 않는 프로필 업데이트 시 새 프로필 생성")
    void updateNonExistingProfileCreatesNewProfile() {
        // given
        Long userId = 1L;
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .nickname("new_profile")
                .build();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        // when
        ProfileResponse response = profileService.updateProfile(userId, request);

        // then
        assertNotNull(response);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    @DisplayName("상담 횟수 증가 테스트")
    void incrementCounselingCount() {
        // given
        Long userId = 1L;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when
        profileService.incrementCounselingCount(userId);

        // then
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(mockProfile).incrementCounselingCount();
    }

    @Test
    @DisplayName("평균 평점 업데이트 테스트")
    void updateAvgRating() {
        // given
        Long userId = 1L;
        double rating = 4.5;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when
        profileService.updateAvgRating(userId, rating);

        // then
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(mockProfile).updateRating(rating);
    }

    @Test
    @DisplayName("응답 시간 업데이트 테스트 - 여러 시간")
    void updateResponseTimes() {
        // given
        Long userId = 1L;
        List<Integer> responseTimes = Arrays.asList(120, 180, 90);

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when
        profileService.updateResponseTimes(userId, responseTimes);

        // then
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(mockProfile).addMultipleResponseTimes(responseTimes);
    }

    @Test
    @DisplayName("응답 시간 업데이트 테스트 - 빈 리스트")
    void updateResponseTimesWithEmptyList() {
        // given
        Long userId = 1L;
        List<Integer> responseTimes = Collections.emptyList();

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when
        profileService.updateResponseTimes(userId, responseTimes);

        // then
        verify(userService).findUserById(userId);
        verify(profileRepository).findByUserId(userId);
        verify(mockProfile).addMultipleResponseTimes(responseTimes);
    }

    @Test
    @DisplayName("프로필이 있는 경우 조회")
    void getExistingProfile() {
        // given
        Long userId = 1L;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(mockProfile));

        // when
        profileService.incrementCounselingCount(userId);

        // then
        verify(profileRepository).findByUserId(userId);
        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    @DisplayName("프로필이 없는 경우 새 프로필 생성")
    void createNewProfileWhenNotExists() {
        // given
        Long userId = 1L;

        when(userService.findUserById(userId)).thenReturn(mockUser);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        // when
        profileService.incrementCounselingCount(userId);

        // then
        verify(profileRepository).findByUserId(userId);
        verify(profileRepository).save(any(Profile.class));
    }
}
