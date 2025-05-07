package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.notification.dto.ReviewCreatedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.domain.Tag;
import com.mindmate.mindmate_server.review.dto.ProfileReviewSummaryResponse;
import com.mindmate.mindmate_server.review.dto.ReviewListResponse;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRedisRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.ProfileService;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewRedisRepository reviewRedisRepository;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private UserService userService;
    @Mock
    private ProfileService profileService;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private ReviewServiceImpl reviewService;
    @InjectMocks
    private ReviewDataService reviewDataService;

    @Mock
    private User reviewer;
    @Mock
    private User reviewedUser;
    @Mock
    private Profile reviewerProfile;
    @Mock
    private Profile reviewedProfile;
    @Mock
    private ChatRoom chatRoom;
    @Mock
    private Review review;
    @Mock
    private ReviewRequest reviewRequest;
    @Mock
    private EvaluationTag evaluationTag;

    @BeforeEach
    void setUp() {
        when(reviewer.getId()).thenReturn(1L);
        when(reviewedUser.getId()).thenReturn(2L);

        when(reviewerProfile.getNickname()).thenReturn("리뷰어닉네임");
        when(reviewer.getProfile()).thenReturn(reviewerProfile);

        when(reviewedProfile.getId()).thenReturn(2L);
        when(reviewedProfile.getAvgRating()).thenReturn(4.5);
        when(reviewedProfile.getNickname()).thenReturn("리뷰대상닉네임");

        when(reviewedUser.getProfile()).thenReturn(reviewedProfile);

        when(chatRoom.getId()).thenReturn(1L);
        when(chatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.CLOSED);

        when(chatRoom.isListener(reviewer)).thenReturn(false);
        when(chatRoom.isSpeaker(reviewer)).thenReturn(true);
        when(chatRoom.getListener()).thenReturn(reviewedUser);
        when(chatRoom.getSpeaker()).thenReturn(reviewer);

        when(reviewRequest.getChatRoomId()).thenReturn(1L);
        when(reviewRequest.getRating()).thenReturn(5);
        when(reviewRequest.getComment()).thenReturn("아주 어메이징한 상담이었습니다");
        when(reviewRequest.getTags()).thenReturn(Arrays.asList("응답이 빨라요", "공감을 잘해줘요"));

        when(review.getId()).thenReturn(1L);
        when(review.getRating()).thenReturn(5);
        when(review.getComment()).thenReturn("아주 어메이징한 상담이었습니다");
        when(review.getReviewer()).thenReturn(reviewer);
        when(review.getReviewedProfile()).thenReturn(reviewedProfile);
        when(review.getChatRoom()).thenReturn(chatRoom);
        when(review.getCreatedAt()).thenReturn(LocalDateTime.now());

        List<EvaluationTag> tags = new ArrayList<>();
        when(evaluationTag.getTagContent()).thenReturn(Tag.RESPONSIVE);
        tags.add(evaluationTag);
        when(review.getReviewTags()).thenReturn(tags);
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReview_success() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // When
        ReviewResponse response = reviewService.createReview(1L, reviewRequest);

        // Then
        assertNotNull(response);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRedisRepository).deleteAllProfileCaches(reviewedProfile.getId());
        verify(profileService).incrementCounselingCount(reviewer.getId());
        verify(profileService).updateAvgRating(reviewer.getId(), reviewRequest.getRating());

        ArgumentCaptor<ReviewCreatedNotificationEvent> notificationCaptor =
                ArgumentCaptor.forClass(ReviewCreatedNotificationEvent.class);
        verify(notificationService).processNotification(notificationCaptor.capture());

        ReviewCreatedNotificationEvent capturedEvent = notificationCaptor.getValue();
        assertEquals(reviewedUser.getId(), capturedEvent.getRecipientId());
        assertEquals(review.getId(), capturedEvent.getReviewId());
        assertEquals(reviewerProfile.getNickname(), capturedEvent.getReviewerName());
    }

    @Test
    @DisplayName("잘못된 평점 - 범위 초과")
    void createReview_invalidRating_tooHigh() {
        // Given
        when(reviewRequest.getRating()).thenReturn(6);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.INVALID_RATING_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("잘못된 평점 - 범위 미만")
    void createReview_invalidRating_tooLow() {
        // Given
        when(reviewRequest.getRating()).thenReturn(0);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.INVALID_RATING_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("채팅방이 종료되지 않은 경우")
    void createReview_chatRoomNotClosed() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(chatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ChatErrorCode.CHAT_ROOM_NOT_CLOSED, exception.getErrorCode());
    }

    @Test
    @DisplayName("자신에 대한 리뷰 작성")
    void createReview_selfReview() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(chatRoom.getListener()).thenReturn(reviewer);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.SELF_REVIEW_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("중복 리뷰")
    void createReview_duplicateReview() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(true);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.REVIEW_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("잘못된 태그 타입")
    void createReview_invalidTagType() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(false);
        when(reviewRequest.getTags()).thenReturn(Arrays.asList("의사소통이 명확해요")); // This is a SPEAKER tag

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.INVALID_REVIEW_TAGS, exception.getErrorCode());
    }

    @Test
    @DisplayName("존재하지 않는 태그")
    void createReview_nonexistentTag() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(false);

        when(reviewRequest.getTags()).thenReturn(Arrays.asList("존재하지 않는 태그"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.INVALID_REVIEW_TAGS, exception.getErrorCode());
    }

    @Test
    @DisplayName("프로필이 없는 사용자에 대한 리뷰")
    void createReview_userWithoutProfile() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(false);

        // User without profile
        when(reviewedUser.getProfile()).thenReturn(null);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ProfileErrorCode.PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("동시성 이슈 발생")
    void createReview_concurrencyIssue() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        // Simulate optimistic locking failure
        doThrow(OptimisticLockingFailureException.class)
                .when(profileService).incrementCounselingCount(anyLong());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.REVIEW_SUBMISSION_CONFLICT, exception.getErrorCode());
    }

    @Test
    @DisplayName("최근순 정렬")
    void getProfileReviews_latestSort() {
        // Given
        Long profileId = 2L;
        int page = 0;
        int size = 10;
        String sortType = "latest";

        Page<Review> reviewPage = new PageImpl<>(Collections.singletonList(review));

        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(eq(reviewedProfile), any(Pageable.class)))
                .thenReturn(reviewPage);

        // When
        Page<ReviewResponse> response = reviewService.getProfileReviews(profileId, page, size, sortType);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(reviewRepository).findByReviewedProfileOrderByCreatedAtDesc(eq(reviewedProfile), any(Pageable.class));
    }

    @Test
    @DisplayName("높은 평점순 정렬")
    void getProfileReviews_highestRatingSort() {
        // Given
        Long profileId = 2L;
        int page = 0;
        int size = 10;
        String sortType = "highest_rating";

        Page<Review> reviewPage = new PageImpl<>(Collections.singletonList(review));

        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.findByReviewedProfileOrderByRatingDesc(eq(reviewedProfile), any(Pageable.class)))
                .thenReturn(reviewPage);

        // When
        Page<ReviewResponse> response = reviewService.getProfileReviews(profileId, page, size, sortType);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(reviewRepository).findByReviewedProfileOrderByRatingDesc(eq(reviewedProfile), any(Pageable.class));
    }

    @Test
    @DisplayName("낮은 평점순 정렬")
    void getProfileReviews_lowestRatingSort() {
        // Given
        Long profileId = 2L;
        int page = 0;
        int size = 10;
        String sortType = "lowest_rating";

        Page<Review> reviewPage = new PageImpl<>(Collections.singletonList(review));

        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.findByReviewedProfileOrderByRatingAsc(eq(reviewedProfile), any(Pageable.class)))
                .thenReturn(reviewPage);

        // When
        Page<ReviewResponse> response = reviewService.getProfileReviews(profileId, page, size, sortType);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(reviewRepository).findByReviewedProfileOrderByRatingAsc(eq(reviewedProfile), any(Pageable.class));
    }

    @Test
    @DisplayName("리뷰 조회 성공")
    void getReview_success() {
        // Given
        Long reviewId = 1L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // When
        ReviewResponse response = reviewService.getReview(reviewId);

        // Then
        assertNotNull(response);
        verify(reviewRepository).findById(reviewId);
    }

    @Test
    @DisplayName("리뷰 찾을 수 없음")
    void getReview_notFound() {
        // Given
        Long reviewId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.getReview(reviewId);
        });

        assertEquals(ReviewErrorCode.REVIEW_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("채팅방 리뷰 조회 성공")
    void getChatRoomReviews_success() {
        // Given
        Long chatRoomId = 1L;
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(chatRoom);
        when(reviewRepository.findByChatRoom(chatRoom)).thenReturn(Collections.singletonList(review));

        // When
        List<ReviewResponse> responses = reviewService.getChatRoomReviews(chatRoomId);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(reviewRepository).findByChatRoom(chatRoom);
    }

    @Test
    @DisplayName("채팅방 리뷰 없음")
    void getChatRoomReviews_emptyList() {
        // Given
        Long chatRoomId = 1L;
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(chatRoom);
        when(reviewRepository.findByChatRoom(chatRoom)).thenReturn(Collections.emptyList());

        // When
        List<ReviewResponse> responses = reviewService.getChatRoomReviews(chatRoomId);

        // Then
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(reviewRepository).findByChatRoom(chatRoom);
    }

    @Test
    @DisplayName("리뷰 가능한 경우")
    void canReview_true() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 1L;

        when(userService.findUserById(userId)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(false);

        // When
        boolean canReview = reviewService.canReview(userId, chatRoomId);

        // Then
        assertTrue(canReview);
    }

    @Test
    @DisplayName("채팅방이 종료되지 않은 경우")
    void canReview_chatRoomNotClosed() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 1L;

        when(userService.findUserById(userId)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(chatRoom);
        when(chatRoom.getChatRoomStatus()).thenReturn(ChatRoomStatus.ACTIVE);

        // When
        boolean canReview = reviewService.canReview(userId, chatRoomId);

        // Then
        assertFalse(canReview);
    }

    @Test
    @DisplayName("사용자가 채팅방에 속하지 않은 경우")
    void canReview_userNotInChatRoom() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 1L;
        User otherUser = mock(User.class);

        when(userService.findUserById(userId)).thenReturn(otherUser);
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(chatRoom);
        when(chatRoom.isListener(otherUser)).thenReturn(false);
        when(chatRoom.isSpeaker(otherUser)).thenReturn(false);

        // When
        boolean canReview = reviewService.canReview(userId, chatRoomId);

        // Then
        assertFalse(canReview);
    }

    @Test
    @DisplayName("이미 리뷰를 작성한 경우")
    void canReview_alreadyReviewed() {
        // Given
        Long userId = 1L;
        Long chatRoomId = 1L;

        when(userService.findUserById(userId)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(chatRoomId)).thenReturn(chatRoom);
        when(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).thenReturn(true);

        // When
        boolean canReview = reviewService.canReview(userId, chatRoomId);

        // Then
        assertFalse(canReview);
    }

    @Test
    @DisplayName("캐시된 리뷰 요약 조회")
    void getProfileReviewSummary_cached() {
        // Given
        Long profileId = 2L;
        ProfileReviewSummaryResponse cachedSummary = mock(ProfileReviewSummaryResponse.class);

        when(reviewRedisRepository.getReviewSummary(profileId)).thenReturn(cachedSummary);

        // When
        ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(profileId);

        // Then
        assertNotNull(response);
        assertSame(cachedSummary, response);
        verify(reviewRedisRepository).getReviewSummary(profileId);
        verify(profileService, never()).findProfileById(anyLong());
    }

    @Test
    @DisplayName("캐시되지 않은 리뷰 요약 조회")
    void getProfileReviewSummary_notCached() {
        // Given
        Long profileId = 2L;

        when(reviewRedisRepository.getReviewSummary(profileId)).thenReturn(null);
        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.countByReviewedProfile(reviewedProfile)).thenReturn(5L);

        List<Object[]> tagCounts = new ArrayList<>();
        tagCounts.add(new Object[]{"응답이 빨라요", 3L});
        when(reviewRepository.countAllTagsByProfile(reviewedProfile)).thenReturn(tagCounts);

        Page<Review> recentReviews = new PageImpl<>(Collections.singletonList(review));
        when(reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(eq(reviewedProfile), any(Pageable.class)))
                .thenReturn(recentReviews);

        // When
        ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(profileId);

        // Then
        assertNotNull(response);
        assertEquals(5, response.getTotalReviews());
        assertTrue(response.getTagCounts().containsKey("응답이 빨라요"));
        assertEquals(3, response.getTagCounts().get("응답이 빨라요"));
        verify(reviewRedisRepository).saveReviewSummary(eq(profileId), any(ProfileReviewSummaryResponse.class));
    }

    @Test
    @DisplayName("캐시되지 않은 태그 카운트 조회")
    void getProfileReviewSummary_notCachedTagCounts() {
        // Given
        Long profileId = 2L;

        when(reviewRedisRepository.getReviewSummary(profileId)).thenReturn(null);
        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.countByReviewedProfile(reviewedProfile)).thenReturn(5L);

        when(reviewRedisRepository.getTagCounts(profileId)).thenReturn(null);

        List<Object[]> tagCounts = new ArrayList<>();
        tagCounts.add(new Object[]{"응답이 빨라요", 3L});
        when(reviewRepository.countAllTagsByProfile(reviewedProfile)).thenReturn(tagCounts);

        Page<Review> recentReviews = new PageImpl<>(Collections.singletonList(review));
        when(reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(eq(reviewedProfile), any(Pageable.class)))
                .thenReturn(recentReviews);

        // When
        ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(profileId);

        // Then
        assertNotNull(response);
        assertEquals(5, response.getTotalReviews());
        assertTrue(response.getTagCounts().containsKey("응답이 빨라요"));
        assertEquals(3, response.getTagCounts().get("응답이 빨라요"));
        verify(reviewRedisRepository).saveTagCounts(eq(profileId), anyMap());
        verify(reviewRedisRepository).saveReviewSummary(eq(profileId), any(ProfileReviewSummaryResponse.class));
    }

    @Test
    @DisplayName("빈 태그 리스트 처리")
    void getProfileReviewSummary_emptyTags() {
        // Given
        Long profileId = 2L;

        when(reviewRedisRepository.getReviewSummary(profileId)).thenReturn(null);
        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.countByReviewedProfile(reviewedProfile)).thenReturn(5L);

        when(reviewRepository.countAllTagsByProfile(reviewedProfile)).thenReturn(Collections.emptyList());

        Page<Review> recentReviews = new PageImpl<>(Collections.singletonList(review));
        when(reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(eq(reviewedProfile), any(Pageable.class)))
                .thenReturn(recentReviews);

        // When
        ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(profileId);

        // Then
        assertNotNull(response);
        assertTrue(response.getTagCounts().isEmpty());
    }

    @Test
    @DisplayName("프로필별 태그 카운트 조회")
    void getTagCountsByProfileId_success() {
        // Given
        Long profileId = 2L;
        List<Object[]> tagCountResults = new ArrayList<>();
        tagCountResults.add(new Object[]{Tag.RESPONSIVE, 3L});
        tagCountResults.add(new Object[]{Tag.EMPATHETIC, 2L});

        when(reviewRepository.countTagsByProfileId(profileId)).thenReturn(tagCountResults);

        // When
        Map<String, Integer> tagCounts = reviewDataService.getTagCountsByProfileId(profileId);

        // Then
        assertNotNull(tagCounts);
        assertEquals(2, tagCounts.size());
        assertEquals(3, tagCounts.get("응답이 빨라요"));
        assertEquals(2, tagCounts.get("공감을 잘해줘요"));
        verify(reviewRepository).countTagsByProfileId(profileId);
    }

    @Test
    @DisplayName("태그 없는 경우")
    void getTagCountsByProfileId_noTags() {
        // Given
        Long profileId = 2L;
        when(reviewRepository.countTagsByProfileId(profileId)).thenReturn(Collections.emptyList());

        // When
        Map<String, Integer> tagCounts = reviewDataService.getTagCountsByProfileId(profileId);

        // Then
        assertNotNull(tagCounts);
        assertTrue(tagCounts.isEmpty());
        verify(reviewRepository).countTagsByProfileId(profileId);
    }

    @Test
    @DisplayName("사용자별 최근 리뷰 조회")
    void getRecentReviewsByUserId_success() {
        // Given
        Long userId = 2L;
        int limit = 3;

        when(reviewRepository.findRecentReviewsByRevieweeId(eq(userId), any(Pageable.class)))
                .thenReturn(Collections.singletonList(review));

        // When
        List<ReviewResponse> recentReviews = reviewDataService.getRecentReviewsByUserId(userId, limit);

        // Then
        assertNotNull(recentReviews);
        assertEquals(1, recentReviews.size());
        assertEquals(review.getId(), recentReviews.get(0).getId());
        assertEquals(review.getRating(), recentReviews.get(0).getRating());
        verify(reviewRepository).findRecentReviewsByRevieweeId(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("사용자별 최근 리뷰 없음")
    void getRecentReviewsByUserId_empty() {
        // Given
        Long userId = 2L;
        int limit = 3;

        when(reviewRepository.findRecentReviewsByRevieweeId(eq(userId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<ReviewResponse> recentReviews = reviewDataService.getRecentReviewsByUserId(userId, limit);

        // Then
        assertNotNull(recentReviews);
        assertTrue(recentReviews.isEmpty());
        verify(reviewRepository).findRecentReviewsByRevieweeId(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("사용자별 평균 평점 조회")
    void getAverageRatingByUserId_success() {
        // Given
        Long userId = 2L;
        double expectedRating = 4.5;

        when(reviewRepository.calculateAverageRatingByRevieweeId(userId))
                .thenReturn(Optional.of(expectedRating));

        // When
        Double avgRating = reviewDataService.getAverageRatingByUserId(userId);

        // Then
        assertNotNull(avgRating);
        assertEquals(expectedRating, avgRating);
        verify(reviewRepository).calculateAverageRatingByRevieweeId(userId);
    }

    @Test
    @DisplayName("평점 없는 경우 기본값 반환")
    void getAverageRatingByUserId_none() {
        // Given
        Long userId = 2L;

        when(reviewRepository.calculateAverageRatingByRevieweeId(userId))
                .thenReturn(Optional.empty());

        // When
        Double avgRating = reviewDataService.getAverageRatingByUserId(userId);

        // Then
        assertNotNull(avgRating);
        assertEquals(0.0, avgRating);
        verify(reviewRepository).calculateAverageRatingByRevieweeId(userId);
    }

    @Test
    @DisplayName("리뷰 ID로 찾기 성공")
    void findReviewById_success() {
        // Given
        Long reviewId = 1L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // When & Then
        assertDoesNotThrow(() -> reviewService.findReviewById(reviewId));
        verify(reviewRepository).findById(reviewId);
    }

    @Test
    @DisplayName("리뷰 ID로 찾기 실패")
    void findReviewById_notFound() {
        // Given
        Long reviewId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.findReviewById(reviewId);
        });

        assertEquals(ReviewErrorCode.REVIEW_NOT_FOUND, exception.getErrorCode());
        verify(reviewRepository).findById(reviewId);
    }

    @Test
    @DisplayName("프로필 메트릭 업데이트 성공")
    void updateProfileMetrics_success() {
        // Given
        Long userId = 1L;
        double rating = 4.5;

        // When
        assertDoesNotThrow(() -> reviewService.updateProfileMetrics(userId, rating));

        // Then
        verify(profileService).incrementCounselingCount(userId);
        verify(profileService).updateAvgRating(userId, rating);
    }

    @Test
    @DisplayName("프로필 메트릭 업데이트 실패 - 동시성 이슈")
    void updateProfileMetrics_concurrencyIssue() {
        // Given
        Long userId = 1L;
        double rating = 4.5;

        doThrow(OptimisticLockingFailureException.class)
                .when(profileService).incrementCounselingCount(userId);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.updateProfileMetrics(userId, rating);
        });

        assertEquals(ReviewErrorCode.REVIEW_SUBMISSION_CONFLICT, exception.getErrorCode());
        verify(profileService).incrementCounselingCount(userId);
    }
}