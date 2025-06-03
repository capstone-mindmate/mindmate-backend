package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ProfileErrorCode;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.notification.dto.ReviewCreatedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.domain.Tag;
import com.mindmate.mindmate_server.review.dto.*;
import com.mindmate.mindmate_server.review.repository.ReviewRedisRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.ProfileImage;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.ProfileService;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 테스트")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewRedisRepository reviewRedisRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private UserService userService;
    @Mock private ProfileService profileService;
    @Mock private NotificationService notificationService;

    @InjectMocks private ReviewServiceImpl reviewService;
    @InjectMocks private ReviewDataService reviewDataService;

    private User reviewer;
    private User reviewedUser;
    private Profile reviewerProfile;
    private Profile reviewedProfile;
    private ProfileImage profileImage;
    private Matching matching;
    private ChatRoom chatRoom;
    private Review review;
    private ReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        setupUsers();
        setupProfiles();
        setupChatRoom();
        setupReviewRequest();
        setupReview();
    }

    private void setupUsers() {
        reviewer = createUser(1L, "reviewer@test.com", RoleType.ROLE_PROFILE);
        reviewedUser = createUser(2L, "reviewed@test.com", RoleType.ROLE_PROFILE);
    }

    private void setupProfiles() {
        profileImage = createProfileImage(1L, "test-image.jpg");
        reviewerProfile = createProfile(1L, "리뷰어닉네임", reviewer, profileImage);
        reviewedProfile = createProfile(2L, "리뷰대상닉네임", reviewedUser, profileImage);

        ReflectionTestUtils.setField(reviewer, "profile", reviewerProfile);
        ReflectionTestUtils.setField(reviewedUser, "profile", reviewedProfile);
    }

    private void setupChatRoom() {
        matching = createMatching(1L, reviewer, reviewedUser, InitiatorType.SPEAKER);
        chatRoom = createChatRoom(1L, matching, ChatRoomStatus.CLOSED);
    }

    private void setupReviewRequest() {
        reviewRequest = ReviewRequest.builder()
                .chatRoomId(1L)
                .rating(5)
                .comment("아주 어메이징한 상담이었습니다")
                .tags(Arrays.asList("응답이 빨라요", "공감을 잘해줘요"))
                .anonymous(false)
                .build();
    }

    private void setupReview() {
        review = createReview(1L, reviewer, reviewedProfile, chatRoom, 5, "아주 어메이징한 상담이었습니다", false);

        List<EvaluationTag> tags = new ArrayList<>(Arrays.asList(
                createEvaluationTag(1L, review, Tag.RESPONSIVE),
                createEvaluationTag(2L, review, Tag.EMPATHETIC)
        ));
        ReflectionTestUtils.setField(review, "reviewTags", tags);
    }

    @Nested
    @DisplayName("리뷰 생성 테스트")
    class CreateReviewTest {

        @Test
        @DisplayName("정상적인 리뷰 생성")
        void createReview_Success() {
            // given
            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(review);

            // when
            ReviewResponse response = reviewService.createReview(1L, reviewRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getComment()).isEqualTo("아주 어메이징한 상담이었습니다");

            then(reviewRepository).should().save(any(Review.class));

            then(reviewRedisRepository).should().deleteAllProfileCaches(reviewedProfile.getId());

            then(reviewRedisRepository).should().incrementTagCount(reviewedProfile.getId(), "응답이 빨라요");
            then(reviewRedisRepository).should().incrementTagCount(reviewedProfile.getId(), "공감을 잘해줘요");

            then(profileService).should().updateAvgRating(1L, 5);

            ArgumentCaptor<ReviewCreatedNotificationEvent> eventCaptor =
                    ArgumentCaptor.forClass(ReviewCreatedNotificationEvent.class);
            then(notificationService).should().processNotification(eventCaptor.capture());

            ReviewCreatedNotificationEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getRecipientId()).isEqualTo(2L);
            assertThat(capturedEvent.getReviewId()).isEqualTo(1L);
            assertThat(capturedEvent.getReviewerName()).isEqualTo("리뷰어닉네임");
        }

        @Test
        @DisplayName("익명 리뷰 생성")
        void createReview_Anonymous() {
            // given
            ReviewRequest anonymousRequest = ReviewRequest.builder()
                    .chatRoomId(1L)
                    .rating(4)
                    .comment("익명 리뷰입니다")
                    .tags(Arrays.asList("응답이 빨라요"))
                    .anonymous(true)
                    .build();

            Review anonymousReview = createReview(2L, reviewer, reviewedProfile, chatRoom, 4, "익명 리뷰입니다", true);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(anonymousReview);

            // when
            ReviewResponse response = reviewService.createReview(1L, anonymousRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            then(reviewRepository).should().save(reviewCaptor.capture());
            Review savedReview = reviewCaptor.getValue();
            assertThat(savedReview.isAnonymous()).isTrue();
        }

        @Test
        @DisplayName("태그 없이 리뷰 생성")
        void createReview_WithoutTags() {
            // given
            ReviewRequest noTagsRequest = ReviewRequest.builder()
                    .chatRoomId(1L)
                    .rating(4)
                    .comment("태그 없는 리뷰")
                    .tags(Collections.emptyList())
                    .anonymous(false)
                    .build();

            Review noTagsReview = createReview(3L, reviewer, reviewedProfile, chatRoom, 4, "태그 없는 리뷰", false);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(noTagsReview);

            // when
            ReviewResponse response = reviewService.createReview(1L, noTagsRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(3L);

            then(reviewRedisRepository).should(never()).incrementTagCount(anyLong(), anyString());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 6, -1, 100})
        @DisplayName("잘못된 평점 값으로 리뷰 생성 실패")
        void createReview_InvalidRating(int invalidRating) {
            // given
            ReviewRequest invalidRequest = ReviewRequest.builder()
                    .chatRoomId(1L)
                    .rating(invalidRating)
                    .comment("테스트")
                    .tags(Collections.emptyList())
                    .anonymous(false)
                    .build();

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, invalidRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.INVALID_RATING_VALUE);
        }

        @Test
        @DisplayName("종료되지 않은 채팅방에서 리뷰 생성 실패")
        void createReview_ChatRoomNotClosed() {
            // given
            Matching activeMatching = createMatching(2L, reviewer, reviewedUser, InitiatorType.SPEAKER);
            ChatRoom activeChatRoom = createChatRoom(2L, activeMatching, ChatRoomStatus.ACTIVE);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(activeChatRoom);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, reviewRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_CLOSED);
        }

        @Test
        @DisplayName("자기 자신에 대한 리뷰 생성 실패")
        void createReview_SelfReview() {
            // given
            Matching selfMatching = createMatching(3L, reviewer, reviewer, InitiatorType.SPEAKER);
            ChatRoom selfChatRoom = createChatRoom(3L, selfMatching, ChatRoomStatus.CLOSED);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(selfChatRoom);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, reviewRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }

        @Test
        @DisplayName("중복 리뷰 생성 실패")
        void createReview_DuplicateReview() {
            // given
            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, reviewRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("존재하지 않는 태그로 리뷰 생성 실패")
        void createReview_InvalidTag() {
            // given
            ReviewRequest invalidTagRequest = ReviewRequest.builder()
                    .chatRoomId(1L)
                    .rating(5)
                    .comment("테스트")
                    .tags(Arrays.asList("존재하지 않는 태그"))
                    .anonymous(false)
                    .build();

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, invalidTagRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.INVALID_REVIEW_TAGS);
        }

        @Test
        @DisplayName("프로필이 없는 사용자에 대한 리뷰 생성 실패")
        void createReview_UserWithoutProfile() {
            // given
            User userWithoutProfile = createUser(3L, "no-profile@test.com", RoleType.ROLE_USER);
            Matching noProfileMatching = createMatching(4L, reviewer, userWithoutProfile, InitiatorType.SPEAKER);
            ChatRoom chatRoomWithNoProfile = createChatRoom(4L, noProfileMatching, ChatRoomStatus.CLOSED);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoomWithNoProfile);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoomWithNoProfile, reviewer)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, reviewRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ProfileErrorCode.PROFILE_NOT_FOUND);
        }

        @Test
        @DisplayName("동시성 이슈 발생 시 리뷰 생성 실패")
        void createReview_ConcurrencyIssue() {
            // given
            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(review);

            willThrow(OptimisticLockingFailureException.class)
                    .given(profileService).updateAvgRating(anyLong(), anyDouble());

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(1L, reviewRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_SUBMISSION_CONFLICT);
        }

        @Test
        @DisplayName("리스너가 스피커에 대한 리뷰 생성")
        void createReview_ListenerToSpeaker() {
            // given
            Matching listenerToSpeakerMatching = createMatching(5L, reviewedUser, reviewer, InitiatorType.SPEAKER);
            ChatRoom listenerToSpeakerChatRoom = createChatRoom(5L, listenerToSpeakerMatching, ChatRoomStatus.CLOSED);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(listenerToSpeakerChatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(listenerToSpeakerChatRoom, reviewer)).willReturn(false);
            given(reviewRepository.save(any(Review.class))).willReturn(review);

            // when
            ReviewResponse response = reviewService.createReview(1L, reviewRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("채팅방에 속하지 않은 사용자의 리뷰 생성 실패")
        void createReview_UserNotInChatRoom() {
            // given
            User outsideUser = createUser(3L, "outside@test.com", RoleType.ROLE_PROFILE);
            Profile outsideProfile = createProfile(3L, "외부사용자", outsideUser, profileImage);
            ReflectionTestUtils.setField(outsideUser, "profile", outsideProfile);

            given(userService.findUserById(3L)).willReturn(outsideUser);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(3L, reviewRequest))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ChatErrorCode.USER_NOT_IN_CHAT);
        }
    }

    @Nested
    @DisplayName("리뷰 조회 테스트")
    class GetReviewTest {

        @ParameterizedTest
        @ValueSource(strings = {"latest", "highest_rating", "lowest_rating", "unknown"})
        @DisplayName("정렬 타입별 프로필 리뷰 조회")
        void getProfileReviews_WithDifferentSortTypes(String sortType) {
            // given
            Long profileId = 2L;
            Page<Review> reviewPage = new PageImpl<>(Arrays.asList(review));

            given(profileService.findProfileById(profileId)).willReturn(reviewedProfile);

            switch (sortType) {
                case "highest_rating":
                    given(reviewRepository.findByReviewedProfileOrderByRatingDesc(eq(reviewedProfile), any(Pageable.class)))
                            .willReturn(reviewPage);
                    break;
                case "lowest_rating":
                    given(reviewRepository.findByReviewedProfileOrderByRatingAsc(eq(reviewedProfile), any(Pageable.class)))
                            .willReturn(reviewPage);
                    break;
                default:
                    given(reviewRepository.findByReviewedProfileOrderByCreatedAtDesc(eq(reviewedProfile), any(Pageable.class)))
                            .willReturn(reviewPage);
                    break;
            }

            // when
            Page<ReviewResponse> response = reviewService.getProfileReviews(profileId, 0, 10, sortType);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하는 리뷰 조회 성공")
        void getReview_Success() {
            // given
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when
            ReviewResponse response = reviewService.getReview(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getComment()).isEqualTo("아주 어메이징한 상담이었습니다");
        }

        @Test
        @DisplayName("존재하지 않는 리뷰 조회 실패")
        void getReview_NotFound() {
            // given
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.getReview(999L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("채팅방 리뷰 조회 성공")
        void getChatRoomReviews_Success() {
            // given
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.findByChatRoom(chatRoom)).willReturn(Arrays.asList(review));

            // when
            List<ReviewResponse> responses = reviewService.getChatRoomReviews(1L);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getChatRoomId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("채팅방에 리뷰가 없는 경우")
        void getChatRoomReviews_Empty() {
            // given
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.findByChatRoom(chatRoom)).willReturn(Collections.emptyList());

            // when
            List<ReviewResponse> responses = reviewService.getChatRoomReviews(1L);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("findReviewById 성공")
        void findReviewById_Success() {
            // given
            given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

            // when
            Review foundReview = reviewService.findReviewById(1L);

            // then
            assertThat(foundReview).isNotNull();
            assertThat(foundReview.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("findReviewById 실패")
        void findReviewById_NotFound() {
            // given
            given(reviewRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewService.findReviewById(999L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("리뷰 권한 확인 테스트")
    class CanReviewTest {

        @Test
        @DisplayName("리뷰 작성 가능한 경우")
        void canReview_True() {
            // given
            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(false);

            // when
            boolean canReview = reviewService.canReview(1L, 1L);

            // then
            assertThat(canReview).isTrue();
        }

        @Test
        @DisplayName("채팅방이 종료되지 않은 경우 리뷰 불가")
        void canReview_ChatRoomNotClosed() {
            // given
            Matching activeMatching = createMatching(6L, reviewer, reviewedUser, InitiatorType.SPEAKER);
            ChatRoom activeChatRoom = createChatRoom(6L, activeMatching, ChatRoomStatus.ACTIVE);

            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(activeChatRoom);

            // when
            boolean canReview = reviewService.canReview(1L, 1L);

            // then
            assertThat(canReview).isFalse();
        }

        @Test
        @DisplayName("사용자가 채팅방에 속하지 않은 경우 리뷰 불가")
        void canReview_UserNotInChatRoom() {
            // given
            User otherUser = createUser(3L, "other@test.com", RoleType.ROLE_PROFILE);

            given(userService.findUserById(3L)).willReturn(otherUser);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);

            // when
            boolean canReview = reviewService.canReview(3L, 1L);

            // then
            assertThat(canReview).isFalse();
        }

        @Test
        @DisplayName("이미 리뷰를 작성한 경우 리뷰 불가")
        void canReview_AlreadyReviewed() {
            // given
            given(userService.findUserById(1L)).willReturn(reviewer);
            given(chatRoomService.findChatRoomById(1L)).willReturn(chatRoom);
            given(reviewRepository.existsByChatRoomAndReviewer(chatRoom, reviewer)).willReturn(true);

            // when
            boolean canReview = reviewService.canReview(1L, 1L);

            // then
            assertThat(canReview).isFalse();
        }
    }

    @Nested
    @DisplayName("프로필 리뷰 요약 테스트")
    class ProfileReviewSummaryTest {

        @Test
        @DisplayName("캐시된 리뷰 요약 조회")
        void getProfileReviewSummary_Cached() {
            // given
            ProfileReviewSummaryResponse cachedSummary = createProfileReviewSummary();
            given(reviewRedisRepository.getReviewSummary(2L)).willReturn(cachedSummary);

            // when
            ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(2L);

            // then
            assertThat(response).isSameAs(cachedSummary);
            then(profileService).should(never()).findProfileById(anyLong());
            then(reviewRepository).should(never()).countByReviewedProfile(any());
        }

        @Test
        @DisplayName("캐시되지 않은 리뷰 요약 조회")
        void getProfileReviewSummary_NotCached() {
            // given
            given(reviewRedisRepository.getReviewSummary(2L)).willReturn(null);
            given(profileService.findProfileById(2L)).willReturn(reviewedProfile);
            given(reviewRepository.countByReviewedProfile(reviewedProfile)).willReturn(5L);

            given(reviewRedisRepository.getTagCounts(2L)).willReturn(null);

            List<Object[]> tagCounts = Arrays.asList(
                    new Object[]{Tag.RESPONSIVE, 3L},
                    new Object[]{Tag.EMPATHETIC, 2L}
            );
            given(reviewRepository.countAllTagsByProfile(reviewedProfile)).willReturn(tagCounts);

            Page<Review> recentReviews = new PageImpl<>(Arrays.asList(review));
            given(reviewRepository.findRecentReviewsByReviewedProfileWithProfileImage(
                    eq(reviewedProfile), any(Pageable.class))).willReturn(recentReviews);

            // when
            ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(2L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalReviews()).isEqualTo(5);
            assertThat(response.getTagCounts()).hasSize(2);
            assertThat(response.getTagCounts().get("응답이 빨라요")).isEqualTo(3);
            assertThat(response.getTagCounts().get("공감을 잘해줘요")).isEqualTo(2);
            assertThat(response.getRecentReviews()).hasSize(1);

            then(reviewRedisRepository).should().saveTagCounts(eq(2L), any(Map.class));
            then(reviewRedisRepository).should().saveReviewSummary(eq(2L), any(ProfileReviewSummaryResponse.class));
        }

        @Test
        @DisplayName("캐시된 태그 카운트가 있는 경우")
        void getProfileReviewSummary_WithCachedTagCounts() {
            // given
            Map<String, Integer> cachedTagCounts = Map.of(
                    "응답이 빨라요", 5,
                    "공감을 잘해줘요", 3
            );

            given(reviewRedisRepository.getReviewSummary(2L)).willReturn(null);
            given(profileService.findProfileById(2L)).willReturn(reviewedProfile);
            given(reviewRepository.countByReviewedProfile(reviewedProfile)).willReturn(8L);
            given(reviewRedisRepository.getTagCounts(2L)).willReturn(cachedTagCounts);

            Page<Review> recentReviews = new PageImpl<>(Arrays.asList(review));
            given(reviewRepository.findRecentReviewsByReviewedProfileWithProfileImage(
                    eq(reviewedProfile), any(Pageable.class))).willReturn(recentReviews);

            // when
            ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(2L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalReviews()).isEqualTo(8);
            assertThat(response.getTagCounts()).isEqualTo(cachedTagCounts);

            then(reviewRepository).should(never()).countAllTagsByProfile(any());
            then(reviewRedisRepository).should(never()).saveTagCounts(anyLong(), any());
        }

        @Test
        @DisplayName("빈 태그 카운트 처리")
        void getProfileReviewSummary_EmptyTagCounts() {
            // given
            given(reviewRedisRepository.getReviewSummary(2L)).willReturn(null);
            given(profileService.findProfileById(2L)).willReturn(reviewedProfile);
            given(reviewRepository.countByReviewedProfile(reviewedProfile)).willReturn(0L);
            given(reviewRedisRepository.getTagCounts(2L)).willReturn(Collections.emptyMap());

            Page<Review> emptyReviews = new PageImpl<>(Collections.emptyList());
            given(reviewRepository.findRecentReviewsByReviewedProfileWithProfileImage(
                    eq(reviewedProfile), any(Pageable.class))).willReturn(emptyReviews);

            // when
            ProfileReviewSummaryResponse response = reviewService.getProfileReviewSummary(2L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalReviews()).isEqualTo(0);
            assertThat(response.getTagCounts()).isEmpty();
            assertThat(response.getRecentReviews()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ReviewDataService 테스트")
    class ReviewDataServiceTest {

        @Test
        @DisplayName("사용자별 최근 리뷰 조회 성공")
        void getRecentReviewsByUserId_Success() {
            // given
            List<Review> recentReviews = Arrays.asList(review);
            given(reviewRepository.findRecentReviewsByRevieweeId(eq(2L), any(Pageable.class)))
                    .willReturn(recentReviews);

            // when
            List<ReviewResponse> responses = reviewDataService.getRecentReviewsByUserId(2L, 3);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getRating()).isEqualTo(5);
            assertThat(responses.get(0).getComment()).isEqualTo("아주 어메이징한 상담이었습니다");

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            then(reviewRepository).should().findRecentReviewsByRevieweeId(eq(2L), pageableCaptor.capture());
            Pageable capturedPageable = pageableCaptor.getValue();
            assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
            assertThat(capturedPageable.getPageSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("사용자별 최근 리뷰 조회 - 빈 결과")
        void getRecentReviewsByUserId_EmptyResult() {
            // given
            given(reviewRepository.findRecentReviewsByRevieweeId(eq(2L), any(Pageable.class)))
                    .willReturn(Collections.emptyList());

            // when
            List<ReviewResponse> responses = reviewDataService.getRecentReviewsByUserId(2L, 5);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("사용자별 평균 평점 조회 성공")
        void getAverageRatingByUserId_Success() {
            // given
            given(reviewRepository.calculateAverageRatingByRevieweeId(2L))
                    .willReturn(Optional.of(4.5));

            // when
            Double avgRating = reviewDataService.getAverageRatingByUserId(2L);

            // then
            assertThat(avgRating).isEqualTo(4.5);
        }

        @Test
        @DisplayName("평점이 없는 경우 기본값 반환")
        void getAverageRatingByUserId_DefaultValue() {
            // given
            given(reviewRepository.calculateAverageRatingByRevieweeId(2L))
                    .willReturn(Optional.empty());

            // when
            Double avgRating = reviewDataService.getAverageRatingByUserId(2L);

            // then
            assertThat(avgRating).isEqualTo(0.0);
        }

        @Test
        @DisplayName("프로필별 태그 카운트 조회 성공")
        void getTagCountsByProfileId_Success() {
            // given
            List<Object[]> tagCountResults = Arrays.asList(
                    new Object[]{Tag.RESPONSIVE, 3L},
                    new Object[]{Tag.EMPATHETIC, 2L},
                    new Object[]{Tag.TRUSTWORTHY, 1L}
            );
            given(reviewRepository.countTagsByProfileId(2L)).willReturn(tagCountResults);

            // when
            Map<String, Integer> tagCounts = reviewDataService.getTagCountsByProfileId(2L);

            // then
            assertThat(tagCounts).hasSize(3);
            assertThat(tagCounts.get("응답이 빨라요")).isEqualTo(3);
            assertThat(tagCounts.get("공감을 잘해줘요")).isEqualTo(2);
            assertThat(tagCounts.get("신뢰할 수 있는 대화였어요")).isEqualTo(1);
        }

        @Test
        @DisplayName("프로필별 태그 카운트 조회 - 빈 결과")
        void getTagCountsByProfileId_EmptyResult() {
            // given
            given(reviewRepository.countTagsByProfileId(2L)).willReturn(Collections.emptyList());

            // when
            Map<String, Integer> tagCounts = reviewDataService.getTagCountsByProfileId(2L);

            // then
            assertThat(tagCounts).isEmpty();
        }

        @Test
        @DisplayName("다양한 limit 값으로 최근 리뷰 조회")
        void getRecentReviewsByUserId_DifferentLimits() {
            // given
            Review review2 = createReview(2L, reviewer, reviewedProfile, chatRoom, 4, "두번째 리뷰", false);
            Review review3 = createReview(3L, reviewer, reviewedProfile, chatRoom, 3, "세번째 리뷰", false);

            List<Review> allReviews = Arrays.asList(review, review2, review3);
            given(reviewRepository.findRecentReviewsByRevieweeId(eq(2L), any(Pageable.class)))
                    .willReturn(allReviews);

            // when
            List<ReviewResponse> responses = reviewDataService.getRecentReviewsByUserId(2L, 10);

            // then
            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(1).getId()).isEqualTo(2L);
            assertThat(responses.get(2).getId()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("updateProfileMetrics 테스트")
    class UpdateProfileMetricsTest {

        @Test
        @DisplayName("프로필 메트릭 업데이트 성공")
        void updateProfileMetrics_Success() {
            // given
            willDoNothing().given(profileService).updateAvgRating(1L, 4.5);

            // when & then
            assertThatCode(() -> reviewService.updateProfileMetrics(1L, 4.5))
                    .doesNotThrowAnyException();

            then(profileService).should().updateAvgRating(1L, 4.5);
        }

        @Test
        @DisplayName("동시성 이슈로 프로필 메트릭 업데이트 실패")
        void updateProfileMetrics_ConcurrencyFailure() {
            // given
            willThrow(OptimisticLockingFailureException.class)
                    .given(profileService).updateAvgRating(1L, 4.5);

            // when & then
            assertThatThrownBy(() -> reviewService.updateProfileMetrics(1L, 4.5))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ReviewErrorCode.REVIEW_SUBMISSION_CONFLICT);
        }
    }

    private User createUser(Long id, String email, RoleType roleType) {
        User user = User.builder()
                .email(email)
                .provider(AuthProvider.GOOGLE)
                .providerId("google_" + id)
                .role(roleType)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ProfileImage createProfileImage(Long id, String imageUrl) {
        User user = createUser(id, "email@test.com", RoleType.ROLE_PROFILE);
        ProfileImage profileImage = ProfileImage.builder()
                .imageUrl(imageUrl)
                .user(user)
                .originalName("test.jpg")
                .storedName("stored_test.jpg")
                .contentType("image/jpeg")
                .fileSize(1024L)
                .build();
        ReflectionTestUtils.setField(profileImage, "id", id);
        return profileImage;
    }

    private Profile createProfile(Long id, String nickname, User user, ProfileImage profileImage) {
        Profile profile = Profile.builder()
                .user(user)
                .nickname(nickname)
                .department("컴퓨터공학과")
                .entranceTime(2020)
                .graduation(false)
                .profileImage(profileImage)
                .agreedToTerms(true)
                .build();
        ReflectionTestUtils.setField(profile, "id", id);
        ReflectionTestUtils.setField(profile, "ratingSum", 22.5);
        ReflectionTestUtils.setField(profile, "counselingCount", 5);
        return profile;
    }

    private Matching createMatching(Long id, User creator, User acceptedUser, InitiatorType creatorRole) {
        Matching matching = Matching.builder()
                .creator(creator)
                .title("테스트 매칭")
                .description("테스트 설명")
                .category(MatchingCategory.ACADEMIC)
                .creatorRole(creatorRole)
                .anonymous(false)
                .allowRandom(true)
                .showDepartment(true)
                .build();
        ReflectionTestUtils.setField(matching, "id", id);
        ReflectionTestUtils.setField(matching, "acceptedUser", acceptedUser);
        return matching;
    }

    private ChatRoom createChatRoom(Long id, Matching matching, ChatRoomStatus status) {
        ChatRoom chatRoom = ChatRoom.builder()
                .matching(matching)
                .build();
        ReflectionTestUtils.setField(chatRoom, "id", id);
        ReflectionTestUtils.setField(chatRoom, "chatRoomStatus", status);
        return chatRoom;
    }

    private Review createReview(Long id, User reviewer, Profile reviewedProfile, ChatRoom chatRoom,
                                int rating, String comment, boolean anonymous) {
        Review review = Review.builder()
                .chatRoom(chatRoom)
                .reviewer(reviewer)
                .reviewedProfile(reviewedProfile)
                .rating(rating)
                .comment(comment)
                .anonymous(anonymous)
                .build();
        ReflectionTestUtils.setField(review, "id", id);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.now());

        ReflectionTestUtils.setField(review, "reviewTags", new ArrayList<>());

        return review;
    }

    private EvaluationTag createEvaluationTag(Long id, Review review, Tag tag) {
        EvaluationTag evaluationTag = EvaluationTag.builder()
                .review(review)
                .tagContent(tag)
                .build();
        ReflectionTestUtils.setField(evaluationTag, "id", id);
        return evaluationTag;
    }

    private ProfileReviewSummaryResponse createProfileReviewSummary() {
        return ProfileReviewSummaryResponse.builder()
                .averageRating(4.5)
                .totalReviews(10)
                .tagCounts(Map.of("응답이 빨라요", 5, "공감을 잘해줘요", 3))
                .recentReviews(Arrays.asList(
                        ReviewListResponse.builder()
                                .id(1L)
                                .reviewerNickname("테스터")
                                .rating(5)
                                .comment("좋았습니다")
                                .build()
                ))
                .build();
    }
}