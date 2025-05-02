package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.*;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.notification.dto.MatchingAcceptedNotificationEvent;
import com.mindmate.mindmate_server.notification.dto.MatchingAppliedNotificationEvent;
import com.mindmate.mindmate_server.notification.service.NotificationService;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.dto.PointUseRequest;
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
import java.util.stream.Collectors;

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

    private LocalDateTime now = LocalDateTime.now();

    private MockedStatic<TransactionSynchronizationManager> mockedStatic;

    @BeforeEach
    void setUp() {
        // 사용자
        when(creator.getId()).thenReturn(1L);
        when(creator.getEmail()).thenReturn("creator@test.com");

        when(applicant.getId()).thenReturn(2L);
        when(applicant.getEmail()).thenReturn("applicant@test.com");

        // 프로필
        when(creatorProfile.getId()).thenReturn(1L);
        when(creatorProfile.getUser()).thenReturn(creator);
        when(creatorProfile.getNickname()).thenReturn("Creator");
        when(creatorProfile.getDepartment()).thenReturn("Computer Science");
        when(creatorProfile.getEntranceTime()).thenReturn(2020);
        when(creatorProfile.isGraduation()).thenReturn(false);
        when(creatorProfile.getCounselingCount()).thenReturn(5);
        when(creatorProfile.getAvgRating()).thenReturn(4.5);

        when(creator.getProfile()).thenReturn(creatorProfile);

        when(applicantProfile.getId()).thenReturn(2L);
        when(applicantProfile.getUser()).thenReturn(applicant);
        when(applicantProfile.getNickname()).thenReturn("Applicant");
        when(applicantProfile.getDepartment()).thenReturn("Psychology");
        when(applicantProfile.getEntranceTime()).thenReturn(2021);
        when(applicantProfile.isGraduation()).thenReturn(false);
        when(applicantProfile.getCounselingCount()).thenReturn(3);
        when(applicantProfile.getAvgRating()).thenReturn(4.0);

        when(applicant.getProfile()).thenReturn(applicantProfile);

        // 매칭
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

        //채팅방
        when(chatRoom.getId()).thenReturn(1L);
        when(matching.getChatRoom()).thenReturn(chatRoom);

        // 대기자
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
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingAndWaitingUser(matching, applicant))
                    .willReturn(Optional.empty());
            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(waitingUser);

            // when
            Long waitingUserId = matchingService.applyForMatching(2L, 1L, request);

            // then
            assertThat(waitingUserId).isEqualTo(1L);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findByMatchingAndWaitingUser(matching, applicant);
            verify(waitingUserRepository).save(any(WaitingUser.class));
            verify(redisMatchingService).incrementUserActiveMatchingCount(2L);
            verify(pointService).usePoints(eq(2L), any(PointUseRequest.class));
            verify(notificationService).processNotification(any(MatchingAppliedNotificationEvent.class));
        }

        @Test
        @DisplayName("포인트 필요한 매칭 신청 시 포인트 차감")
        void usePointsWhenApplying() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingAndWaitingUser(matching, applicant))
                    .willReturn(Optional.empty());
            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(waitingUser);

            // when
            matchingService.applyForMatching(2L, 1L, request);

            // then
            verify(pointService).usePoints(eq(2L), argThat(pointRequest ->
                    pointRequest.getAmount() == 100 &&
                            pointRequest.getReasonType() == PointReasonType.COUNSELING_REQUESTED));
        }

        @Test
        @DisplayName("자신의 매칭에 신청 시 예외 발생")
        void applyForOwnMatching() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            // when & then
            assertThatThrownBy(() -> matchingService.applyForMatching(1L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.CANNOT_APPLY_TO_OWN_MATCHING);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository, never()).save(any(WaitingUser.class));
            verify(pointService, never()).usePoints(anyLong(), any(PointUseRequest.class));
        }

        @Test
        @DisplayName("이미 닫힌 매칭에 신청 시 예외 발생")
        void applyForClosedMatching() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            Matching closedMatching = mock(Matching.class);
            when(closedMatching.getId()).thenReturn(2L);
            when(closedMatching.getCreator()).thenReturn(creator);
            when(closedMatching.isCreator(any(User.class))).thenReturn(false);
            when(closedMatching.isOpen()).thenReturn(false);
            when(closedMatching.getStatus()).thenReturn(MatchingStatus.CANCELED);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(closedMatching));

            // when & then
            assertThatThrownBy(() -> matchingService.applyForMatching(2L, 2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_ALREADY_CLOSED);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(2L);
            verify(waitingUserRepository, never()).save(any(WaitingUser.class));
            verify(pointService, never()).usePoints(anyLong(), any(PointUseRequest.class));
        }

        @Test
        @DisplayName("이미 신청한 매칭에 재신청 시 예외 발생")
        void alreadyApplied() {
            // given
            WaitingUserRequest request = new WaitingUserRequest("I want to participate", false);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingAndWaitingUser(matching, applicant))
                    .willReturn(Optional.of(waitingUser));

            // when & then
            assertThatThrownBy(() -> matchingService.applyForMatching(2L, 1L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.ALREADY_APPLIED_TO_MATCHING);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findByMatchingAndWaitingUser(matching, applicant);
            verify(waitingUserRepository, never()).save(any(WaitingUser.class));
            verify(pointService, never()).usePoints(anyLong(), any(PointUseRequest.class));
        }
    }

    @Nested
    @DisplayName("매칭 수락 테스트")
    class AcceptMatchingTest {

        @Test
        @DisplayName("매칭 수락 성공")
        void acceptMatchingSuccess() {
            // given
            List<WaitingUser> waitingUsers = new ArrayList<>();
            waitingUsers.add(waitingUser);

            WaitingUser otherWaitingUser = mock(WaitingUser.class);
            when(otherWaitingUser.getId()).thenReturn(2L);
            when(otherWaitingUser.getWaitingUser()).thenReturn(applicant);
            when(otherWaitingUser.getStatus()).thenReturn(WaitingStatus.PENDING);
            when(otherWaitingUser.getMatching()).thenReturn(matching);
            waitingUsers.add(otherWaitingUser);

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

            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findById(1L);
            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);
            verify(matching).acceptMatching(applicant);
            verify(waitingUser).accept();
            verify(notificationService).processNotification(any(MatchingAcceptedNotificationEvent.class));
            verify(matchingEventProducer).publishMatchingAccepted(any(MatchingAcceptedEvent.class));
            verify(redisMatchingService).cleanupMatchingKeys(matching);
        }

        @Test
        @DisplayName("잘못된 매칭 ID로 수락 시 예외 발생")
        void acceptInvalidMatchingId() {
            // given
            given(matchingRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> matchingService.acceptMatching(1L, 999L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_NOT_FOUND);

            verify(matchingRepository).findById(999L);
            verify(waitingUserRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("잘못된 대기 사용자 ID로 수락 시 예외 발생")
        void acceptInvalidWaitingUserId() {
            // given
            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> matchingService.acceptMatching(1L, 1L, 999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.WAITING_NOT_FOUND);

            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findById(999L);
        }

        @Test
        @DisplayName("다른 매칭의 대기 사용자를 수락 시 예외 발생")
        void acceptInvalidMatchingWaiting() {
            // given
            Matching otherMatching = mock(Matching.class);
            when(otherMatching.getId()).thenReturn(2L);

            WaitingUser otherWaitingUser = mock(WaitingUser.class);
            when(otherWaitingUser.getId()).thenReturn(2L);
            when(otherWaitingUser.getMatching()).thenReturn(otherMatching);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findById(2L)).willReturn(Optional.of(otherWaitingUser));

            // when & then
            assertThatThrownBy(() -> matchingService.acceptMatching(1L, 1L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.INVALID_MATCHING_WAITING);

            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findById(2L);
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
                    false,
                    true
            );

            MatchingServiceImpl spyMatchingService = spy(matchingService);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0);
            given(redisMatchingService.getRandomMatching(eq(applicant), eq(InitiatorType.LISTENER)))
                    .willReturn(1L);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));

            WaitingUser autoWaitingUser = mock(WaitingUser.class);
            when(autoWaitingUser.getId()).thenReturn(1L);
            when(autoWaitingUser.getWaitingUser()).thenReturn(applicant);
            when(autoWaitingUser.getMatching()).thenReturn(matching);
            when(autoWaitingUser.getMatchingType()).thenReturn(MatchingType.AUTO_RANDOM);
            when(autoWaitingUser.isAnonymous()).thenReturn(false);

            given(waitingUserRepository.save(any(WaitingUser.class))).willReturn(autoWaitingUser);

            doReturn(1L).when(spyMatchingService).acceptMatching(1L, 1L, 1L);

            // when
            Long chatRoomId = spyMatchingService.autoMatchApply(2L, request);

            // then
            assertThat(chatRoomId).isEqualTo(1L); // ChatRoom ID 반환 확인

            verify(userService).findUserById(2L);
            verify(redisMatchingService).getUserActiveMatchingCount(2L);
            verify(redisMatchingService).getRandomMatching(applicant, InitiatorType.LISTENER);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).save(any(WaitingUser.class));
            verify(spyMatchingService).acceptMatching(anyLong(), anyLong(), anyLong());
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
        }

        @Test
        @DisplayName("활성 매칭 수 초과 시 예외 발생")
        void autoMatchExceedActiveMatchings() {
            // given
            AutoMatchingRequest request = new AutoMatchingRequest(
                    InitiatorType.LISTENER,
                    false,
                    true
            );

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(3);

            // when & then
            assertThatThrownBy(() -> matchingService.autoMatchApply(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_LIMIT_EXCEED);

            verify(userService).findUserById(2L);
            verify(redisMatchingService).getUserActiveMatchingCount(2L);
            verify(redisMatchingService, never()).getRandomMatching(any(), any());
        }

        @Test
        @DisplayName("매칭 가능한 상대가 없을 때 예외 발생")
        void autoMatchNoMatchingAvailable() {
            // given
            AutoMatchingRequest request = new AutoMatchingRequest(
                    InitiatorType.LISTENER,
                    false,
                    true
            );

            given(userService.findUserById(2L)).willReturn(applicant);
            given(redisMatchingService.getUserActiveMatchingCount(2L)).willReturn(0);
            given(redisMatchingService.getRandomMatching(eq(applicant), eq(InitiatorType.LISTENER)))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> matchingService.autoMatchApply(2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NO_MATCHING_AVAILABLE);

            verify(userService).findUserById(2L);
            verify(redisMatchingService).getUserActiveMatchingCount(2L);
            verify(redisMatchingService).getRandomMatching(applicant, InitiatorType.LISTENER);
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

            Matching otherUserMatching = mock(Matching.class);
            when(otherUserMatching.getId()).thenReturn(2L);
            when(otherUserMatching.getCreator()).thenReturn(applicant);
            when(otherUserMatching.isCreator(creator)).thenReturn(false);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(otherUserMatching));

            // when & then
            assertThatThrownBy(() -> matchingService.updateMatching(1L, 2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NOT_MATCHING_OWNER);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(2L);
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

            Matching canceledMatching = mock(Matching.class);
            when(canceledMatching.getId()).thenReturn(2L);
            when(canceledMatching.getCreator()).thenReturn(creator);
            when(canceledMatching.isCreator(creator)).thenReturn(true);
            when(canceledMatching.isOpen()).thenReturn(false);
            when(canceledMatching.getStatus()).thenReturn(MatchingStatus.CANCELED);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(canceledMatching));

            // when & then
            assertThatThrownBy(() -> matchingService.updateMatching(1L, 2L, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_ALREADY_CLOSED);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(2L);
        }
    }

    @Nested
    @DisplayName("대기 취소 테스트")
    class CancelWaitingTest {

        @Test
        @DisplayName("대기 취소 성공")
        void cancelWaitingSuccess() {
            // given
            doReturn(true).when(applicant).addCancelCount();

            given(userService.findUserById(2L)).willReturn(applicant);
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));

            // when
            matchingService.cancelWaiting(2L, 1L);

            // then
            verify(userService).findUserById(2L);
            verify(waitingUserRepository).findById(1L);
            verify(applicant).addCancelCount();
            verify(waitingUserRepository).delete(waitingUser);
            verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
        }

        @Test
        @DisplayName("대기 소유자가 아닌 사용자가 취소 시 예외 발생")
        void cancelWaitingNotOwner() {
            // given
            User otherUser = mock(User.class);
            when(otherUser.getId()).thenReturn(3L);

            given(userService.findUserById(3L)).willReturn(otherUser);
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));

            // when & then
            assertThatThrownBy(() -> matchingService.cancelWaiting(3L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.NOT_MATCHING_OWNER);

            verify(userService).findUserById(3L);
            verify(waitingUserRepository).findById(1L);
            verify(waitingUserRepository, never()).delete(any(WaitingUser.class));
        }

        @Test
        @DisplayName("이미 처리된 대기 취소 시 예외 발생")
        void cancelProcessedWaiting() {
            // given
            WaitingUser acceptedWaitingUser = mock(WaitingUser.class);
            when(acceptedWaitingUser.getId()).thenReturn(2L);
            when(acceptedWaitingUser.getWaitingUser()).thenReturn(applicant);
            when(acceptedWaitingUser.getMatching()).thenReturn(matching);
            when(acceptedWaitingUser.getStatus()).thenReturn(WaitingStatus.ACCEPTED);
            when(acceptedWaitingUser.isOwner(applicant)).thenReturn(true);
            when(acceptedWaitingUser.isPending()).thenReturn(false);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(waitingUserRepository.findById(2L)).willReturn(Optional.of(acceptedWaitingUser));

            // when & then
            assertThatThrownBy(() -> matchingService.cancelWaiting(2L, 2L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.CANNOT_CANCEL_PROCESSED_WAITING);

            verify(userService).findUserById(2L);
            verify(waitingUserRepository).findById(2L);
            verify(waitingUserRepository, never()).delete(any(WaitingUser.class));
        }

        @Test
        @DisplayName("일일 취소 한도 초과 시 예외 발생")
        void dailyCancelLimitExceeded() {
            // given
            doReturn(false).when(applicant).addCancelCount();

            given(userService.findUserById(2L)).willReturn(applicant);
            given(waitingUserRepository.findById(1L)).willReturn(Optional.of(waitingUser));

            // when & then
            assertThatThrownBy(() -> matchingService.cancelWaiting(2L, 1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.DAILY_LIMIT_CANCEL_EXCEED);

            verify(userService).findUserById(2L);
            verify(waitingUserRepository).findById(1L);
            verify(applicant).addCancelCount();
            verify(waitingUserRepository, never()).delete(any(WaitingUser.class));
        }
    }

    @Nested
    @DisplayName("매칭 조회 테스트")
    class GetMatchingsTest {

        @Test
        @DisplayName("모든 매칭 조회 성공")
        void getAllMatchings() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.findByStatusOrderByCreatedAtDesc(any(MatchingStatus.class), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getMatchings(pageable, null, null, null);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).findByStatusOrderByCreatedAtDesc(MatchingStatus.OPEN, pageable);
        }

        @Test
        @DisplayName("카테고리별 매칭 조회 성공")
        void getMatchingsByCategory() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.findByStatusAndCategoryOrderByCreatedAtDesc(
                    any(MatchingStatus.class), any(MatchingCategory.class), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getMatchings(pageable, MatchingCategory.CAREER, null, null);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).findByStatusAndCategoryOrderByCreatedAtDesc(
                    MatchingStatus.OPEN, MatchingCategory.CAREER, pageable);
        }

        @Test
        @DisplayName("학과별 매칭 조회 성공")
        void getMatchingsByDepartment() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.findOpenMatchingsByDepartment(
                    any(MatchingStatus.class), anyString(), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getMatchings(pageable, null, "Computer Science", null);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).findOpenMatchingsByDepartment(
                    MatchingStatus.OPEN, "Computer Science", pageable);
        }

        @Test
        @DisplayName("필요한 역할별 매칭 조회 성공")
        void getMatchingsByRequiredRole() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.findByStatusAndCreatorRoleOrderByCreatedAtDesc(
                    any(MatchingStatus.class), any(InitiatorType.class), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getMatchings(pageable, null, null, InitiatorType.LISTENER);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).findByStatusAndCreatorRoleOrderByCreatedAtDesc(
                    MatchingStatus.OPEN, InitiatorType.SPEAKER, pageable);
        }

        @Test
        @DisplayName("카테고리와 학과별 매칭 조회 성공")
        void getMatchingsByCategoryAndDepartment() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.findByStatusAndCategoryAndDepartment(
                    any(MatchingStatus.class), any(MatchingCategory.class), anyString(), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getMatchings(
                    pageable, MatchingCategory.CAREER, "Computer Science", null);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).findByStatusAndCategoryAndDepartment(
                    MatchingStatus.OPEN, MatchingCategory.CAREER, "Computer Science", pageable);
        }
    }

    @Nested
    @DisplayName("매칭 세부 정보 조회 테스트")
    class GetMatchingDetailTest {

        @Test
        @DisplayName("매칭 세부 정보 조회 성공")
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
        @DisplayName("존재하지 않는 매칭 세부 정보 조회 시 예외 발생")
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

            given(matchingRepository.searchMatchingsWithFilters(
                    any(MatchingStatus.class), anyString(), any(MatchingCategory.class),
                    anyString(), any(InitiatorType.class), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.searchMatchings(pageable, request);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(matchingRepository).searchMatchingsWithFilters(
                    eq(MatchingStatus.OPEN), eq("Test"), eq(MatchingCategory.CAREER),
                    eq("Computer Science"), eq(InitiatorType.SPEAKER), eq(pageable));
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
    @DisplayName("사용자 매칭 이력 조회 테스트")
    class GetUserMatchingHistoryTest {

        @Test
        @DisplayName("생성한 매칭 이력 조회 성공")
        void getUserMatchingHistoryAsCreator() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);


            when(matching.getStatus()).thenReturn(MatchingStatus.MATCHED);
            when(matching.getAcceptedUser()).thenReturn(applicant);
            LocalDateTime matchedAt = LocalDateTime.now();
            when(matching.getMatchedAt()).thenReturn(matchedAt);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findByCreatorAndStatusOrderByMatchedAtDesc(
                    any(User.class), any(MatchingStatus.class), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getUserMatchingHistory(1L, pageable, false);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(userService).findUserById(1L);
            verify(matchingRepository).findByCreatorAndStatusOrderByMatchedAtDesc(
                    creator, MatchingStatus.MATCHED, pageable);
        }

        @Test
        @DisplayName("참여한 매칭 이력 조회 성공")
        void getUserMatchingHistoryAsParticipant() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = List.of(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            when(matching.getStatus()).thenReturn(MatchingStatus.MATCHED);
            when(matching.getAcceptedUser()).thenReturn(applicant);
            LocalDateTime matchedAt = LocalDateTime.now();
            when(matching.getMatchedAt()).thenReturn(matchedAt);

            given(userService.findUserById(2L)).willReturn(applicant);
            given(matchingRepository.findByAcceptedUserAndStatusOrderByMatchedAtDesc(
                    any(User.class), any(MatchingStatus.class), any(Pageable.class)))
                    .willReturn(matchingPage);

            // when
            Page<MatchingResponse> responses = matchingService.getUserMatchingHistory(2L, pageable, true);

            // then
            assertThat(responses.getTotalElements()).isEqualTo(1);

            verify(userService).findUserById(2L);
            verify(matchingRepository).findByAcceptedUserAndStatusOrderByMatchedAtDesc(
                    applicant, MatchingStatus.MATCHED, pageable);
        }
    }

    @Nested
    @DisplayName("매칭 취소 테스트")
    class CancelMatchingTest {

        @Test
        @DisplayName("매칭 취소 성공")
        void cancelMatchingSuccess() {
            // given
            List<WaitingUser> waitingUsers = new ArrayList<>();
            waitingUsers.add(waitingUser);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(1L)).willReturn(Optional.of(matching));
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);

            // when
            matchingService.cancelMatching(1L, 1L);

            // then
            verify(userService).findUserById(1L);
            verify(matchingRepository).findById(1L);
            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);
            verify(waitingUser).reject();
            verify(matching).cancelMatching();
            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
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
        void cancelAlreadyCanceled() {
            // given
            Matching canceledMatching = mock(Matching.class);
            when(canceledMatching.getId()).thenReturn(2L);
            when(canceledMatching.getCreator()).thenReturn(creator);
            when(canceledMatching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);
            when(canceledMatching.isCreator(creator)).thenReturn(true);
            when(canceledMatching.isOpen()).thenReturn(false);
            when(canceledMatching.getStatus()).thenReturn(MatchingStatus.CANCELED);

            given(userService.findUserById(1L)).willReturn(creator);
            given(matchingRepository.findById(2L)).willReturn(Optional.of(canceledMatching));

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
            assertThat(result).containsEntry("MENTAL_HEALTH", 0);

            verify(matchingRepository).countMatchingsByUserAndCategory(1L);
        }
    }
}