package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.global.exception.PointErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.notification.dto.MatchingAcceptedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.MatchingAppliedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointRequest;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingServiceImplTest {

    @Mock
    private MatchingRepository matchingRepository;
    @Mock
    private WaitingUserRepository waitingUserRepository;
    @Mock
    private UserService userService;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private RedisMatchingService redisMatchingService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PointService pointService;
    @Mock
    private MatchingEventProducer matchingEventProducer;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    @Mock
    private User creator;
    @Mock
    private User applicant;
    @Mock
    private Matching matching;
    @Mock
    private WaitingUser waitingUser;
    @Mock
    private Profile creatorProfile;
    @Mock
    private Profile applicantProfile;
    @Mock
    private ChatRoom chatRoom;
    @Mock
    private com.mindmate.mindmate_server.user.domain.ProfileImage creatorProfileImage;
    @Mock
    private com.mindmate.mindmate_server.user.domain.ProfileImage applicantProfileImage;

    private LocalDateTime now = LocalDateTime.now();
    private MockedStatic<TransactionSynchronizationManager> mockedStatic;

    @BeforeEach
    void setUp() {
        // 사용자 설정
        when(creator.getId()).thenReturn(1L);
        when(creator.getEmail()).thenReturn("creator@test.com");
        when(applicant.getId()).thenReturn(2L);
        when(applicant.getEmail()).thenReturn("applicant@test.com");

        // 프로필 설정
        when(creatorProfile.getId()).thenReturn(1L);
        when(creatorProfile.getUser()).thenReturn(creator);
        when(creatorProfile.getNickname()).thenReturn("Creator");
        when(creatorProfile.getDepartment()).thenReturn("Computer Science");
        when(creatorProfile.getEntranceTime()).thenReturn(2020);
        when(creatorProfile.isGraduation()).thenReturn(false);
        when(creatorProfile.getCounselingCount()).thenReturn(5);
        when(creatorProfile.getAvgRating()).thenReturn(4.5);
        when(creatorProfile.getProfileImage()).thenReturn(creatorProfileImage);
        when(creatorProfileImage.getImageUrl()).thenReturn("creator_image_url");
        when(creator.getProfile()).thenReturn(creatorProfile);

        when(applicantProfile.getId()).thenReturn(2L);
        when(applicantProfile.getUser()).thenReturn(applicant);
        when(applicantProfile.getNickname()).thenReturn("Applicant");
        when(applicantProfile.getDepartment()).thenReturn("Psychology");
        when(applicantProfile.getEntranceTime()).thenReturn(2021);
        when(applicantProfile.isGraduation()).thenReturn(false);
        when(applicantProfile.getCounselingCount()).thenReturn(3);
        when(applicantProfile.getAvgRating()).thenReturn(4.0);
        when(applicantProfile.getProfileImage()).thenReturn(applicantProfileImage);
        when(applicantProfileImage.getImageUrl()).thenReturn("applicant_image_url");
        when(applicant.getProfile()).thenReturn(applicantProfile);

        // 매칭 설정
        when(matching.getId()).thenReturn(1L);
        when(matching.getTitle()).thenReturn("Test Matching");
        when(matching.getDescription()).thenReturn("This is a test matching");
        when(matching.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(matching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);
        when(matching.isAnonymous()).thenReturn(false);
        when(matching.isAllowRandom()).thenReturn(false);
        when(matching.isShowDepartment()).thenReturn(true);
        when(matching.getCreator()).thenReturn(creator);
        when(matching.getStatus()).thenReturn(MatchingStatus.OPEN);
        when(matching.getCreatedAt()).thenReturn(now);
        when(matching.isOpen()).thenReturn(true);
        when(matching.isCreator(eq(creator))).thenReturn(true);
        when(matching.isCreator(eq(applicant))).thenReturn(false);
        when(matching.getWaitingUsersCount()).thenReturn(1);

        // 채팅방 설정
        when(chatRoom.getId()).thenReturn(1L);
        when(matching.getChatRoom()).thenReturn(chatRoom);

        // 대기자 설정
        when(waitingUser.getId()).thenReturn(1L);
        when(waitingUser.getWaitingUser()).thenReturn(applicant);
        when(waitingUser.getMessage()).thenReturn("I want to participate");
        when(waitingUser.getMatchingType()).thenReturn(MatchingType.MANUAL);
        when(waitingUser.isAnonymous()).thenReturn(false);
        when(waitingUser.getMatching()).thenReturn(matching);
        when(waitingUser.getStatus()).thenReturn(WaitingStatus.PENDING);
        when(waitingUser.isOwner(eq(applicant))).thenReturn(true);
        when(waitingUser.isOwner(eq(creator))).thenReturn(false);
        when(waitingUser.isPending()).thenReturn(true);

        // TransactionSynchronizationManager Mock 설정
        mockedStatic = mockStatic(TransactionSynchronizationManager.class);
        mockedStatic.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                .thenAnswer(invocation -> {
                    TransactionSynchronization synchronization = invocation.getArgument(0);
                    synchronization.afterCommit();
                    return null;
                });
    }

    @AfterEach
    void tearDown() {
        if (mockedStatic != null) {
            mockedStatic.close();
        }
    }

    @Nested
    @DisplayName("매칭 생성 테스트")
    class CreateMatchingTest {

        @Test
        @DisplayName("매칭 생성 성공")
        void createMatchingSuccess() {
            // given
            MatchingCreateRequest request = new MatchingCreateRequest(
                    "Test Matching",
                    "This is a test matching",
                    MatchingCategory.CAREER,
                    InitiatorType.SPEAKER,
                    true,
                    false,
                    false
            );

            given(userService.findUserById(1L)).willReturn(creator);
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(0);
            given(chatRoomService.createChatRoom(any(Matching.class))).willReturn(chatRoom);
            given(matchingRepository.save(any(Matching.class))).willReturn(matching);

            // when
            MatchingCreateResponse response = matchingService.createMatching(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMatchingId()).isEqualTo(1L);
            assertThat(response.getChatRoomId()).isEqualTo(1L);

            verify(userService).findUserById(1L);
            verify(redisMatchingService).getUserActiveMatchingCount(1L);
            verify(chatRoomService).createChatRoom(any(Matching.class));
            verify(matchingRepository, times(1)).save(any(Matching.class));
            verify(redisMatchingService).incrementUserActiveMatchingCount(1L);
        }

        @Test
        @DisplayName("활성 매칭 수 초과 시 예외 발생")
        void exceedActiveMatchings() {
            // given
            MatchingCreateRequest request = new MatchingCreateRequest(
                    "Test Matching",
                    "This is a test matching",
                    MatchingCategory.CAREER,
                    InitiatorType.SPEAKER,
                    true,
                    false,
                    false
            );

            given(userService.findUserById(1L)).willReturn(creator);
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(3);

            // when & then
            assertThatThrownBy(() -> matchingService.createMatching(1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_LIMIT_EXCEED);

            verify(userService).findUserById(1L);
            verify(redisMatchingService).getUserActiveMatchingCount(1L);
            verify(chatRoomService, never()).createChatRoom(any(Matching.class));
            verify(matchingRepository, never()).save(any(Matching.class));
        }

        @Test
        @DisplayName("랜덤 매칭 허용 시 레디스에 추가")
        void addToRedisWhenRandomAllowed() {
            // given
            MatchingCreateRequest request = new MatchingCreateRequest(
                    "Test Matching",
                    "This is a test matching",
                    MatchingCategory.CAREER,
                    InitiatorType.SPEAKER,
                    true,
                    true,  // allowRandom = true
                    false
            );

            when(matching.isAllowRandom()).thenReturn(true);

            given(userService.findUserById(1L)).willReturn(creator);
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(0);
            given(chatRoomService.createChatRoom(any(Matching.class))).willReturn(chatRoom);
            given(matchingRepository.save(any(Matching.class))).willReturn(matching);

            // when
            matchingService.createMatching(1L, request);

            // then
            verify(redisMatchingService).addMatchingToAvailableSet(any(Matching.class));
        }
    }

    @Nested
    @DisplayName("매칭 신청 테스트")
    class ApplyForMatchingTest {

        @Test
        @DisplayName("매칭 신청 성공")
        void applyForMatchingSuccess() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingAndWaitingUser(matching, applicant))
                    .willReturn(Optional.empty());
            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(waitingUser);

            // when
            Long waitingUserId = matchingService.applyForMatching(2L, 1L, request);

            // then
            assertThat(waitingUserId).isEqualTo(1L);

            verify(userService).findUserById(2L);
            verify(redisMatchingService).getUserActiveMatchingCount(2L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findByMatchingAndWaitingUser(matching, applicant);
            verify(waitingUserRepository).save(any(WaitingUser.class));
            verify(redisMatchingService).incrementUserActiveMatchingCount(2L);
            verify(notificationService).processNotification(any(MatchingAppliedNotificationEvent.class));
        }

        @Test
        @DisplayName("자신의 매칭에 신청 시 예외 발생")
        void applyForOwnMatching() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            given(userService.findUserById(1L)).willReturn(creator);
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(0);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when & then
            assertThatThrownBy(() -> matchingService.applyForMatching(1L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.CANNOT_APPLY_TO_OWN_MATCHING);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository, never()).save(any(WaitingUser.class));
        }

        @Test
        @DisplayName("활성 매칭 수 초과 시 신청 불가")
        void applyForMatchingExceedLimit() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching)); // 매칭을 먼저 찾아야 함
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(3);

            // when & then
            assertThatThrownBy(() -> matchingService.applyForMatching(2L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_LIMIT_EXCEED);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(1L);
            verify(redisMatchingService).getUserActiveMatchingCount(2L);
        }
    }

    @Nested
    @DisplayName("매칭 수락 테스트")
    class AcceptMatchingTest {

        @Test
        @DisplayName("매칭 수락 성공 - 생성자가 스피커인 경우")
        void acceptMatchingSuccessWithSpeakerCreator() {
            // given
            List<WaitingUser> waitingUsers = List.of(waitingUser);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);
            given(matchingRepository.save(matching)).willReturn(matching);

            // when
            Long matchingId = matchingService.acceptMatching(1L, 1L, 1L);

            // then
            assertThat(matchingId).isEqualTo(1L);

            verify(pointService).usePoints(eq(1L), argThat(pointRequest ->
                    pointRequest.getTransactionType() == TransactionType.SPEND &&
                            pointRequest.getAmount() == 100 &&
                            pointRequest.getReasonType() == PointReasonType.COUNSELING_REQUESTED));
            verify(waitingUser).accept();
            verify(matching).acceptMatching(applicant);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
            verify(notificationService).processNotification(any(MatchingAcceptedNotificationEvent.class));
        }

        @Test
        @DisplayName("매칭 수락 시 포인트 부족으로 실패")
        void acceptMatchingInsufficientPoints() {
            // given
            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));
            given(pointService.usePoints(eq(1L), any(PointRequest.class)))
                    .willThrow(new CustomException(PointErrorCode.INSUFFICIENT_POINTS));

            // when & then
            assertThatThrownBy(() -> matchingService.acceptMatching(1L, 1L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.INSUFFICIENT_POINTS_FOR_MATCHING);

            verify(waitingUser, never()).accept();
            verify(matching, never()).acceptMatching(any(User.class));
        }
    }

    @Nested
    @DisplayName("자동 매칭 신청 테스트")
    class AutoMatchApplyTest {

        @Test
        @DisplayName("자동 매칭 신청 성공")
        void autoMatchApplySuccess() {
            // given
            AutoMatchingRequest request = new AutoMatchingRequest(
                    InitiatorType.LISTENER,
                    false, true
            );

            MatchingServiceImpl spyMatchingService = spy(matchingService);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0);
            given(redisMatchingService.getRandomMatching(eq(applicant), eq(InitiatorType.LISTENER)))
                    .willReturn(1L);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            WaitingUser autoWaitingUser = mock(WaitingUser.class);
            when(autoWaitingUser.getId()).thenReturn(1L);
            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(autoWaitingUser);

            doReturn(1L).when(spyMatchingService).acceptMatching(1L, 1L, 1L);

            // when
            Long chatRoomId = spyMatchingService.autoMatchApply(2L, request);

            // then
            assertThat(chatRoomId).isEqualTo(1L);

            verify(redisMatchingService).getRandomMatching(applicant, InitiatorType.LISTENER);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
        }

        @Test
        @DisplayName("자동 매칭 가능한 상대가 없을 때 예외 발생")
        void autoMatchNoMatchingAvailable() {
            // given
            AutoMatchingRequest request = new AutoMatchingRequest(
                    InitiatorType.LISTENER,
                    false, true
            );

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0);
            given(redisMatchingService.getRandomMatching(eq(applicant), eq(InitiatorType.LISTENER)))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> matchingService.autoMatchApply(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NO_MATCHING_AVAILABLE);
        }
    }

    @Nested
    @DisplayName("대기 취소 테스트")
    class CancelWaitingTest {

        @Test
        @DisplayName("대기 취소 성공")
        void cancelWaitingSuccess() {
            // given
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));

            // when
            matchingService.cancelWaiting(2L, 1L);

            // then
            verify(waitingUserRepository).findById(1L);
            verify(waitingUserRepository).delete(waitingUser);
            verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
        }

        @Test
        @DisplayName("존재하지 않는 대기 취소 시 예외 발생")
        void cancelWaitingNotFound() {
            // given
            given(waitingUserRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> matchingService.cancelWaiting(2L, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.WAITING_NOT_FOUND);

            verify(waitingUserRepository, never()).delete(any(WaitingUser.class));
        }
    }

    @Nested
    @DisplayName("매칭 조회 테스트")
    class GetMatchingsTest {

        @Test
        @DisplayName("매칭 목록 조회 성공")
        void getMatchingsSuccess() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(waitingUserRepository.findMatchingIdsByWaitingUserId(2L))
                    .willReturn(Collections.emptyList());
            given(matchingRepository.getMatchingsWithFilters(
                    eq(MatchingStatus.OPEN), isNull(), isNull(), isNull(),
                    eq(2L), anyList(), eq(pageable)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getMatchings(
                    2L, pageable, null, null, null);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(waitingUserRepository).findMatchingIdsByWaitingUserId(2L);
            verify(matchingRepository).getMatchingsWithFilters(
                    eq(MatchingStatus.OPEN), isNull(), isNull(), isNull(),
                    eq(2L), anyList(), eq(pageable));
        }
    }

    @Nested
    @DisplayName("매칭 검색 테스트")
    class SearchMatchingsTest {

        @Test
        @DisplayName("매칭 검색 성공")
        void searchMatchingsSuccess() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            MatchingSearchRequest request = new MatchingSearchRequest(
                    "Test",
                    MatchingCategory.CAREER,
                    "Computer Science",
                    InitiatorType.LISTENER
            );

            given(waitingUserRepository.findMatchingIdsByWaitingUserId(2L))
                    .willReturn(Collections.emptyList());
            given(matchingRepository.searchMatchingsWithFilters(
                    eq(MatchingStatus.OPEN), eq("Test"), eq(MatchingCategory.CAREER),
                    eq("Computer Science"), eq(InitiatorType.SPEAKER), eq(2L),
                    anyList(), eq(pageable)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.searchMatchings(2L, pageable, request);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).searchMatchingsWithFilters(
                    eq(MatchingStatus.OPEN), eq("Test"), eq(MatchingCategory.CAREER),
                    eq("Computer Science"), eq(InitiatorType.SPEAKER), eq(2L),
                    anyList(), eq(pageable));
        }
    }

    @Nested
    @DisplayName("생성한 매칭 조회 테스트")
    class GetCreatedMatchingsTest {

        @Test
        @DisplayName("생성한 매칭 조회 성공")
        void getCreatedMatchingsSuccess() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.findByCreatorIdAndStatusOrderByCreatedAtDesc(
                    eq(1L), eq(MatchingStatus.OPEN), eq(pageable)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getCreatedMatchings(1L, pageable);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).findByCreatorIdAndStatusOrderByCreatedAtDesc(
                    1L, MatchingStatus.OPEN, pageable);
        }
    }

    @Nested
    @DisplayName("신청한 매칭 조회 테스트")
    class GetAppliedMatchingsTest {

        @Test
        @DisplayName("신청한 매칭 조회 성공")
        void getAppliedMatchingsSuccess() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<WaitingUser> waitingList = List.of(waitingUser);
            Page<WaitingUser> waitingPage = new PageImpl<>(waitingList, pageable, 1);

            given(waitingUserRepository.findByWaitingUserIdAndMatchingStatusOrderByCreatedAtDesc(
                    eq(2L), eq(MatchingStatus.OPEN), eq(pageable)))
                    .willReturn(waitingPage);

            // when
            Page<AppliedMatchingResponse> responses = matchingService.getAppliedMatchings(2L, pageable);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(waitingUserRepository).findByWaitingUserIdAndMatchingStatusOrderByCreatedAtDesc(
                    2L, MatchingStatus.OPEN, pageable);
        }
    }

    @Nested
    @DisplayName("대기 사용자 조회 테스트")
    class GetWaitingUsersTest {

        @Test
        @DisplayName("대기 사용자 조회 성공")
        void getWaitingUsersSuccess() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<WaitingUser> waitingList = List.of(waitingUser);
            Page<WaitingUser> waitingPage = new PageImpl<>(waitingList, pageable, 1);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingWithWaitingUserProfile(any(Matching.class), any(Pageable.class)))
                    .willReturn(waitingPage);

            // when
            Page<WaitingUserResponse> responses = matchingService.getWaitingUsers(1L, 1L, pageable);

            // then
            assertThat(responses).isNotNull();
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findByMatchingWithWaitingUserProfile(matching, pageable);
        }

        @Test
        @DisplayName("매칭 소유자가 아닌 사용자가 대기 사용자 조회 시 예외 발생")
        void getWaitingUsersNotOwner() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when & then
            assertThatThrownBy(() -> matchingService.getWaitingUsers(2L, 1L, pageable))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NOT_MATCHING_OWNER);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository, never()).findByMatchingWithWaitingUserProfile(any(Matching.class), any(Pageable.class));
        }

        @Test
        @DisplayName("닫힌 매칭의 대기 사용자 조회 시 예외 발생")
        void getWaitingUsersForClosedMatching() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Matching closedMatching = mock(Matching.class);
            when(closedMatching.getId()).thenReturn(2L);
            when(closedMatching.getCreator()).thenReturn(creator);
            when(closedMatching.isCreator(creator)).thenReturn(true);
            when(closedMatching.getStatus()).thenReturn(MatchingStatus.CANCELED);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(closedMatching));

            // when & then
            assertThatThrownBy(() -> matchingService.getWaitingUsers(1L, 2L, pageable))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.INVALID_MATCHING_STATUS);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(2L);
            verify(waitingUserRepository, never()).findByMatchingWithWaitingUserProfile(any(Matching.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("매칭 상세 조회 테스트")
    class GetMatchingDetailTest {

        @Test
        @DisplayName("매칭 상세 조회 성공")
        void getMatchingDetailSuccess() {
            // given
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when
            MatchingDetailResponse response = matchingService.getMatchingDetail(1L);

            // then
            assertThat(response).isNotNull();

            verify(matchingRepository).findById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 매칭 상세 조회 시 예외 발생")
        void getMatchingDetailNotFound() {
            // given
            given(matchingRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> matchingService.getMatchingDetail(999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_NOT_FOUND);

            verify(matchingRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("매칭 업데이트 테스트")
    class UpdateMatchingTest {

        @Test
        @DisplayName("매칭 업데이트 성공")
        void updateMatchingSuccess() {
            // given
            MatchingUpdateRequest request = new MatchingUpdateRequest(
                    "Updated Title",
                    "Updated Description",
                    MatchingCategory.RELATIONSHIP,
                    true,
                    true,
                    false
            );

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when
            MatchingDetailResponse response = matchingService.updateMatching(1L, 1L, request);

            // then
            assertThat(response).isNotNull();

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(1L);
            verify(matching).updateMatchingInfo(
                    eq("Updated Title"),
                    eq("Updated Description"),
                    eq(MatchingCategory.RELATIONSHIP),
                    eq(true),
                    eq(true),
                    eq(false)
            );
        }

        @Test
        @DisplayName("매칭 소유자가 아닌 사용자가 업데이트 시 예외 발생")
        void updateMatchingNotOwner() {
            // given
            MatchingUpdateRequest request = new MatchingUpdateRequest(
                    "Updated Title",
                    "Updated Description",
                    MatchingCategory.RELATIONSHIP,
                    true,
                    true,
                    false
            );

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when & then
            assertThatThrownBy(() -> matchingService.updateMatching(2L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NOT_MATCHING_OWNER);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(1L);
        }

        @Test
        @DisplayName("이미 닫힌 매칭 업데이트 시 예외 발생")
        void updateClosedMatching() {
            // given
            MatchingUpdateRequest request = new MatchingUpdateRequest(
                    "Updated Title",
                    "Updated Description",
                    MatchingCategory.RELATIONSHIP,
                    true,
                    true,
                    false
            );

            Matching closedMatching = mock(Matching.class);
            when(closedMatching.getId()).thenReturn(2L);
            when(closedMatching.getCreator()).thenReturn(creator);
            when(closedMatching.isCreator(creator)).thenReturn(true);
            when(closedMatching.isOpen()).thenReturn(false);
            when(closedMatching.getStatus()).thenReturn(MatchingStatus.CANCELED);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(closedMatching));

            // when & then
            assertThatThrownBy(() -> matchingService.updateMatching(1L, 2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_ALREADY_CLOSED);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(2L);
        }
    }

    @Nested
    @DisplayName("매칭 ID로 찾기 테스트")
    class FindMatchingByIdTest {

        @Test
        @DisplayName("매칭 ID로 찾기 성공")
        void findMatchingByIdSuccess() {
            // given
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when
            Matching result = matchingService.findMatchingById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            verify(matchingRepository).findById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 매칭 ID로 찾기 시 예외 발생")
        void matchingNotFound() {
            // given
            given(matchingRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> matchingService.findMatchingById(999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_NOT_FOUND);

            verify(matchingRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("매칭 취소 테스트")
    class CancelMatchingTest {

        @Test
        @DisplayName("매칭 취소 성공")
        void cancelMatchingSuccess() {
            // given
            List<WaitingUser> waitingUsers = List.of(waitingUser);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);

            // when
            matchingService.cancelMatching(1L, 1L);

            // then
            verify(waitingUser).reject();
            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching).cancelMatching();
            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
            verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
        }

        @Test
        @DisplayName("매칭 소유자가 아닌 사용자가 취소 시 예외 발생")
        void cancelMatchingNotOwner() {
            // given
            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when & then
            assertThatThrownBy(() -> matchingService.cancelMatching(2L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NOT_MATCHING_OWNER);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository, never()).findByMatchingOrderByCreatedAtDesc(any(Matching.class));
        }

        @Test
        @DisplayName("이미 닫힌 매칭 취소 시 예외 발생")
        void cancelAlreadyClosedMatching() {
            // given
            Matching closedMatching = mock(Matching.class);
            when(closedMatching.getId()).thenReturn(2L);
            when(closedMatching.getCreator()).thenReturn(creator);
            when(closedMatching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);
            when(closedMatching.isCreator(creator)).thenReturn(true);
            when(closedMatching.isOpen()).thenReturn(false);
            when(closedMatching.getStatus()).thenReturn(MatchingStatus.CANCELED);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(closedMatching));

            // when & then
            assertThatThrownBy(() -> matchingService.cancelMatching(1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_ALREADY_CLOSED);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(2L);
            verify(waitingUserRepository, never()).findByMatchingOrderByCreatedAtDesc(any(Matching.class));
        }
    }

    @Nested
    @DisplayName("매칭 상태 조회 테스트")
    class getMatchingStatusTest {

        @Test
        @DisplayName("매칭 상태 조회 성공")
        void getMatchingStatusSuccess() {
            // given
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(2);

            // when
            MatchingStatusResponse response = matchingService.getMatchingStatus(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCurrentActiveMatchings()).isEqualTo(2);
            assertThat(response.getMaxActiveMatchings()).isEqualTo(3);
            assertThat(response.isCanCreateMore()).isTrue();

            verify(redisMatchingService).getUserActiveMatchingCount(1L);
        }

        @Test
        @DisplayName("매칭 상태 조회 - 제한에 도달한 경우")
        void getMatchingStatusAtLimit() {
            // given
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(3);

            // when
            MatchingStatusResponse response = matchingService.getMatchingStatus(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCurrentActiveMatchings()).isEqualTo(3);
            assertThat(response.getMaxActiveMatchings()).isEqualTo(3);
            assertThat(response.isCanCreateMore()).isFalse();

            verify(redisMatchingService).getUserActiveMatchingCount(1L);
        }

        @Test
        @DisplayName("매칭 상태 조회 - 활성 매칭이 없는 경우")
        void getMatchingStatusEmpty() {
            // given
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(0);

            // when
            MatchingStatusResponse response = matchingService.getMatchingStatus(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCurrentActiveMatchings()).isEqualTo(0);
            assertThat(response.getMaxActiveMatchings()).isEqualTo(3);
            assertThat(response.isCanCreateMore()).isTrue();

            verify(redisMatchingService).getUserActiveMatchingCount(1L);
        }
    }

    @Nested
    @DisplayName("카테고리별 통계 테스트")
    class CategoryCountsTest {

        @Test
        @DisplayName("카테고리별 매칭 갯수 조회")
        void getCategoryCountsByUserId() {
            // given
            List<Object[]> mockResults = Arrays.asList(
                    new Object[]{MatchingCategory.ACADEMIC, 3L},
                    new Object[]{MatchingCategory.CAREER, 5L}
            );

            given(matchingRepository.countMatchingsByUserAndCategory(1L)).willReturn(mockResults);

            // when
            Map<String, Integer> result = matchingService.getCategoryCountsByUserId(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result).containsEntry("ACADEMIC", 3);
            assertThat(result).containsEntry("CAREER", 5);
            // 나머지 카테고리는 0으로 초기화되어 있어야 함
            assertThat(result).containsEntry("RELATIONSHIP", 0);
            assertThat(result).containsEntry("FINANCIAL", 0);
            assertThat(result).containsEntry("EMPLOYMENT", 0);
            assertThat(result).containsEntry("OTHER", 0);

            verify(matchingRepository).countMatchingsByUserAndCategory(1L);
        }
    }

    @Nested
    @DisplayName("사용자 시나리오 테스트")
    class UserScenarioTest {

        @Test
        @DisplayName("시나리오: 새 사용자가 첫 매칭을 생성하고 상태를 확인")
        void scenarioNewUserFirstMatching() {
            // given - 새 사용자
            MatchingCreateRequest request = new MatchingCreateRequest(
                    "Help with Career Planning",
                    "Need advice on career path",
                    MatchingCategory.CAREER,
                    InitiatorType.SPEAKER,
                    false,
                    true,
                    true
            );

            given(userService.findUserById(1L)).willReturn(creator);
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(0);
            given(chatRoomService.createChatRoom(any(Matching.class))).willReturn(chatRoom);
            given(matchingRepository.save(any(Matching.class))).willReturn(matching);
            when(matching.isAllowRandom()).thenReturn(true);

            // when - 매칭 생성
            MatchingCreateResponse createResponse = matchingService.createMatching(1L, request);

            // then - 매칭 생성 확인
            assertThat(createResponse.getMatchingId()).isEqualTo(1L);
            verify(redisMatchingService).addMatchingToAvailableSet(any(Matching.class));
            verify(redisMatchingService).incrementUserActiveMatchingCount(1L);

            // when - 상태 확인
            given(redisMatchingService.getUserActiveMatchingCount(1L)).willReturn(1);
            MatchingStatusResponse statusResponse = matchingService.getMatchingStatus(1L);

            // then - 상태 확인
            assertThat(statusResponse.getCurrentActiveMatchings()).isEqualTo(1);
            assertThat(statusResponse.isCanCreateMore()).isTrue();
        }

        @Test
        @DisplayName("시나리오: 사용자가 매칭 한도에 도달 후 신청 시도")
        void scenarioUserReachesLimitAndTriesToApply() {
            // given - 이미 3개의 활성 매칭이 있는 사용자
            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching)); // 매칭을 먼저 찾아야 함
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(3);

            WaitingUserRequest request = new WaitingUserRequest("Want to join", false);

            // when & then - 신청 시도하면 예외 발생
            assertThatThrownBy(() -> matchingService.applyForMatching(2L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_LIMIT_EXCEED);

            // when - 상태 확인
            MatchingStatusResponse statusResponse = matchingService.getMatchingStatus(2L);

            // then - 더 이상 생성할 수 없음
            assertThat(statusResponse.getCurrentActiveMatchings()).isEqualTo(3);
            assertThat(statusResponse.isCanCreateMore()).isFalse();
        }

        @Test
        @DisplayName("시나리오: 매칭 완료 후 활성 수 감소 확인")
        void scenarioMatchingCompletionReducesActiveCount() {
            // given - 매칭 수락 준비
            List<WaitingUser> waitingUsers = List.of(waitingUser);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);
            given(matchingRepository.save(matching)).willReturn(matching);

            // when - 매칭 수락
            matchingService.acceptMatching(1L, 1L, 1L);

            // then - Redis 정리 확인
            verify(redisMatchingService).cleanupMatchingKeys(matching);
            verify(notificationService).processNotification(any(MatchingAcceptedNotificationEvent.class));
        }

        @Test
        @DisplayName("시나리오: 매칭 취소 시 모든 대기자의 활성 수 감소")
        void scenarioMatchingCancellationReducesAllActiveCount() {
            // given - 여러 대기자가 있는 매칭
            WaitingUser waitingUser2 = mock(WaitingUser.class);
            when(waitingUser2.getId()).thenReturn(2L);
            when(waitingUser2.getWaitingUser()).thenReturn(applicant);
            when(waitingUser2.getStatus()).thenReturn(WaitingStatus.PENDING);

            User anotherApplicant = mock(User.class);
            when(anotherApplicant.getId()).thenReturn(3L);
            WaitingUser waitingUser3 = mock(WaitingUser.class);
            when(waitingUser3.getId()).thenReturn(3L);
            when(waitingUser3.getWaitingUser()).thenReturn(anotherApplicant);
            when(waitingUser3.getStatus()).thenReturn(WaitingStatus.PENDING);

            List<WaitingUser> waitingUsers = Arrays.asList(waitingUser, waitingUser2, waitingUser3);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);

            // when - 매칭 취소
            matchingService.cancelMatching(1L, 1L);

            // then - 모든 대기자의 활성 수 감소 확인 (applicant는 waitingUser와 waitingUser2에서 중복이므로 2번 호출됨)
            verify(redisMatchingService).decrementUserActiveMatchingCount(1L); // 생성자
            verify(redisMatchingService, times(2)).decrementUserActiveMatchingCount(2L); // 대기자1 (중복)
            verify(redisMatchingService).decrementUserActiveMatchingCount(3L); // 대기자2
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
        }

        @Test
        @DisplayName("시나리오: 자동 매칭 성공 시 즉시 매칭 완료")
        void scenarioAutoMatchingImmediateCompletion() {
            // given
            AutoMatchingRequest request = new AutoMatchingRequest(InitiatorType.LISTENER, false, true);
            MatchingServiceImpl spyMatchingService = spy(matchingService);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0);
            given(redisMatchingService.getRandomMatching(eq(applicant), eq(InitiatorType.LISTENER)))
                    .willReturn(1L);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            WaitingUser autoWaitingUser = mock(WaitingUser.class);
            when(autoWaitingUser.getId()).thenReturn(1L);
            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(autoWaitingUser);

            doReturn(1L).when(spyMatchingService).acceptMatching(1L, 1L, 1L);

            // when
            Long chatRoomId = spyMatchingService.autoMatchApply(2L, request);

            // then
            assertThat(chatRoomId).isEqualTo(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(spyMatchingService).acceptMatching(1L, 1L, 1L);
        }

        @Test
        @DisplayName("시나리오: 대기 취소 후 다시 신청 가능")
        void scenarioWaitingCancellationAllowsReapplication() {
            // given - 대기 취소
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));

            // when - 대기 취소
            matchingService.cancelWaiting(2L, 1L);

            // then - 활성 수 감소 확인
            verify(redisMatchingService).decrementUserActiveMatchingCount(2L);

            // given - 다시 신청 준비
            WaitingUserRequest newRequest = new WaitingUserRequest("Want to join again", false);
            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0); // 취소 후 감소됨
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingAndWaitingUser(matching, applicant))
                    .willReturn(Optional.empty());
            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(waitingUser);

            // when - 다시 신청
            Long newWaitingUserId = matchingService.applyForMatching(2L, 1L, newRequest);

            // then - 신청 성공
            assertThat(newWaitingUserId).isEqualTo(1L);
            verify(redisMatchingService).incrementUserActiveMatchingCount(2L);
        }
    }
}