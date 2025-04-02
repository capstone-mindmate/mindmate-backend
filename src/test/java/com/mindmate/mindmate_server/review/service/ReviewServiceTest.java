package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.dto.ProfileReviewSummaryResponse;
import com.mindmate.mindmate_server.review.dto.ReviewRequest;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRedisRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.ProfileService;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
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
    @InjectMocks
    private ReviewServiceImpl reviewService;
    @Mock
    private User reviewer;
    @Mock
    private User reviewedUser;
    @Mock
    private Profile reviewedProfile;
    @Mock
    private ChatRoom chatRoom;
    @Mock
    private Review review;
    @Mock
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        when(reviewer.getId()).thenReturn(1L);
        when(reviewedUser.getId()).thenReturn(2L);

        Profile reviewerProfile = mock(Profile.class);
        when(reviewerProfile.getNickname()).thenReturn("리뷰어닉네임");
        when(reviewer.getProfile()).thenReturn(reviewerProfile);

        when(reviewedProfile.getId()).thenReturn(1L);
        when(reviewedProfile.getAvgRating()).thenReturn(4.5);

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
        when(review.getReviewTags()).thenReturn(new ArrayList<>());
    }

    //createReview
    @Test
    void createReview() {
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
        verify(reviewRedisRepository).deleteReviewSummaryCache(reviewedProfile.getId());
        verify(reviewedProfile).incrementCounselingCount();
        verify(reviewedProfile).updateAvgRating(reviewRequest.getRating());
    }

    @Test
    void createReviewInvalidRating() {
        // Given
        when(reviewRequest.getRating()).thenReturn(6);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.INVALID_RATING_VALUE, exception.getErrorCode());
    }

    @Test
    void createReviewChatRoomNotClosed() {
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
    void createReviewSelfReview() {
        // Given
        when(userService.findUserById(1L)).thenReturn(reviewer);
        when(chatRoomService.findChatRoomById(1L)).thenReturn(chatRoom);

        when(chatRoom.getListener()).thenReturn(reviewer); // 리스너가 reviewedUser이어야함

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.createReview(1L, reviewRequest);
        });

        assertEquals(ReviewErrorCode.SELF_REVIEW_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void createReviewDuplicateReview() {
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

    //getProfileReviews
    @Test
    void getProfileReviewsLatestSort() {
        // Given
        Long profileId = 1L;
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
    void getProfileReviewsHighestRatingSort() {
        // Given
        Long profileId = 1L;
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
    void getProfileReviewsLowestRatingSor() {
        // Given
        Long profileId = 1L;
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

    // getReview
    @Test
    void getReview() {
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
    void getReviewNotFound() {
        // Given
        Long reviewId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            reviewService.getReview(reviewId);
        });

        assertEquals(ReviewErrorCode.REVIEW_NOT_FOUND, exception.getErrorCode());
    }

    // getChatRoomReviews
    @Test
    void getChatRoomReviews() {
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
    void getChatRoomReviewsEmptyList() {
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

    // canReview
    @Test
    void canReview() {
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
    void canReviewChatRoomNotClosed() {
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
    void canReviewUserNotInChatRoom() {
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
    void canReviewAlreadyReviewed() {
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

    // getProfileReviewSummary
    @Test
    void getProfileReviewSummaryRedisCaching() {
        // Given
        Long profileId = 1L;
        ProfileReviewSummaryResponse cachedSummary = mock(ProfileReviewSummaryResponse.class);

        when(reviewRedisRepository.getReviewSummary(profileId)).thenReturn(cachedSummary);

        // When
        ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(profileId);

        // Then
        assertNotNull(response);
        verify(reviewRedisRepository).getReviewSummary(profileId);
        verify(profileService, never()).findProfileById(anyLong());
    }

    @Test
    void getProfileReviewSummaryDB() {
        // Given
        Long profileId = 1L;

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
    void getProfileReviewSummaryEmptyTags() {
        // Given
        Long profileId = 1L;

        when(reviewRedisRepository.getReviewSummary(profileId)).thenReturn(null);
        when(profileService.findProfileById(profileId)).thenReturn(reviewedProfile);
        when(reviewRepository.countByReviewedProfile(reviewedProfile)).thenReturn(5L);

        // 빈 태그 목록
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

}