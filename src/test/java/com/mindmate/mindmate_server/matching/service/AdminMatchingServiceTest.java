package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.matching.domain.*;
import com.mindmate.mindmate_server.matching.dto.MatchingResponse;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminMatchingServiceTest {

    @Mock
    private MatchingRepository matchingRepository;
    @Mock
    private MatchingService matchingService;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private RedisMatchingService redisMatchingService;
    @Mock
    private WaitingUserRepository waitingUserRepository;

    @InjectMocks
    private AdminMatchingService adminMatchingService;

    @Mock
    private User creator;
    @Mock
    private User applicant1;
    @Mock
    private User applicant2;
    @Mock
    private Profile creatorProfile;
    @Mock
    private Profile applicant1Profile;
    @Mock
    private Profile applicant2Profile;
    @Mock
    private Matching matching;
    @Mock
    private WaitingUser waitingUser1;
    @Mock
    private WaitingUser waitingUser2;
    @Mock
    private ChatRoom chatRoom;

    private LocalDateTime now = LocalDateTime.now();
    private MockedStatic<TransactionSynchronizationManager> mockedStatic;

    @BeforeEach
    void setUp() {
        // 사용자 및 프로필 설정
        when(creator.getId()).thenReturn(1L);
        when(applicant1.getId()).thenReturn(2L);
        when(applicant2.getId()).thenReturn(3L);

        when(creatorProfile.getNickname()).thenReturn("Creator");
        when(creatorProfile.getDepartment()).thenReturn("Computer Science");
        when(creator.getProfile()).thenReturn(creatorProfile);

        when(applicant1Profile.getNickname()).thenReturn("Applicant1");
        when(applicant1Profile.getDepartment()).thenReturn("Psychology");
        when(applicant1.getProfile()).thenReturn(applicant1Profile);

        when(applicant2Profile.getNickname()).thenReturn("Applicant2");
        when(applicant2Profile.getDepartment()).thenReturn("Mathematics");
        when(applicant2.getProfile()).thenReturn(applicant2Profile);

        // 매칭 설정
        when(matching.getId()).thenReturn(1L);
        when(matching.getTitle()).thenReturn("Test Matching");
        when(matching.getDescription()).thenReturn("Test Description");
        when(matching.getCategory()).thenReturn(MatchingCategory.CAREER);
        when(matching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);
        when(matching.getCreator()).thenReturn(creator);
        when(matching.getStatus()).thenReturn(MatchingStatus.OPEN);
        when(matching.isOpen()).thenReturn(true);
        when(matching.getChatRoom()).thenReturn(chatRoom);
        when(matching.getCreatedAt()).thenReturn(now);
        when(matching.isAnonymous()).thenReturn(false);
        when(matching.isAllowRandom()).thenReturn(false);
        when(matching.isShowDepartment()).thenReturn(true);

        // 대기 사용자 설정
        when(waitingUser1.getId()).thenReturn(1L);
        when(waitingUser1.getWaitingUser()).thenReturn(applicant1);
        when(waitingUser1.getStatus()).thenReturn(WaitingStatus.PENDING);
        when(waitingUser1.getMessage()).thenReturn("I want to participate");
        when(waitingUser1.getMatchingType()).thenReturn(MatchingType.MANUAL);
        when(waitingUser1.isAnonymous()).thenReturn(false);

        when(waitingUser2.getId()).thenReturn(2L);
        when(waitingUser2.getWaitingUser()).thenReturn(applicant2);
        when(waitingUser2.getStatus()).thenReturn(WaitingStatus.PENDING);
        when(waitingUser2.getMessage()).thenReturn("I also want to participate");
        when(waitingUser2.getMatchingType()).thenReturn(MatchingType.MANUAL);
        when(waitingUser2.isAnonymous()).thenReturn(false);

        // 채팅룸
        when(chatRoom.getId()).thenReturn(1L);

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
    @DisplayName("관리자 매칭 조회 테스트")
    class GetMatchingsTest {

        @Test
        @DisplayName("카테고리 필터 없이 매칭 조회 성공")
        void getMatchingsWithoutCategoryFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = Arrays.asList(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.getMatchingsWithFilters(
                    eq(MatchingStatus.OPEN),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(pageable)
            )).willReturn(matchingPage);

            // when
            Page<MatchingResponse> result = adminMatchingService.getMatchings(pageable, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);

            verify(matchingRepository).getMatchingsWithFilters(
                    MatchingStatus.OPEN, null, null, null, null, null, pageable);
        }

        @Test
        @DisplayName("특정 카테고리로 매칭 조회 성공")
        void getMatchingsWithCategoryFilter() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = Arrays.asList(matching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 1);

            given(matchingRepository.getMatchingsWithFilters(
                    eq(MatchingStatus.OPEN),
                    eq(MatchingCategory.CAREER),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(pageable)
            )).willReturn(matchingPage);

            // when
            Page<MatchingResponse> result = adminMatchingService.getMatchings(pageable, MatchingCategory.CAREER);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);

            verify(matchingRepository).getMatchingsWithFilters(
                    MatchingStatus.OPEN, MatchingCategory.CAREER, null, null, null, null, pageable);
        }

        @Test
        @DisplayName("빈 결과 조회")
        void getMatchingsEmptyResult() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Matching> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            given(matchingRepository.getMatchingsWithFilters(
                    eq(MatchingStatus.OPEN),
                    eq(MatchingCategory.ACADEMIC),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(pageable)
            )).willReturn(emptyPage);

            // when
            Page<MatchingResponse> result = adminMatchingService.getMatchings(pageable, MatchingCategory.ACADEMIC);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();

            verify(matchingRepository).getMatchingsWithFilters(
                    MatchingStatus.OPEN, MatchingCategory.ACADEMIC, null, null, null, null, pageable);
        }

        @Test
        @DisplayName("다중 카테고리 매칭 조회")
        void getMatchingsMultipleCategories() {
            // given
            Matching careerMatching = mock(Matching.class);
            when(careerMatching.getId()).thenReturn(1L);
            when(careerMatching.getTitle()).thenReturn("Career Matching");
            when(careerMatching.getCategory()).thenReturn(MatchingCategory.CAREER);
            when(careerMatching.getCreator()).thenReturn(creator);
            when(careerMatching.getStatus()).thenReturn(MatchingStatus.OPEN);
            when(careerMatching.getCreatedAt()).thenReturn(now);
            when(careerMatching.getCreatorRole()).thenReturn(InitiatorType.SPEAKER);

            Matching academicMatching = mock(Matching.class);
            when(academicMatching.getId()).thenReturn(2L);
            when(academicMatching.getTitle()).thenReturn("Academic Matching");
            when(academicMatching.getCategory()).thenReturn(MatchingCategory.ACADEMIC);
            when(academicMatching.getCreator()).thenReturn(creator);
            when(academicMatching.getStatus()).thenReturn(MatchingStatus.OPEN);
            when(academicMatching.getCreatedAt()).thenReturn(now);
            when(academicMatching.getCreatorRole()).thenReturn(InitiatorType.LISTENER);

            Pageable pageable = PageRequest.of(0, 10);
            List<Matching> matchingList = Arrays.asList(careerMatching, academicMatching);
            Page<Matching> matchingPage = new PageImpl<>(matchingList, pageable, 2);

            given(matchingRepository.getMatchingsWithFilters(
                    eq(MatchingStatus.OPEN),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(null),
                    eq(pageable)
            )).willReturn(matchingPage);

            // when
            Page<MatchingResponse> result = adminMatchingService.getMatchings(pageable, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);

            verify(matchingRepository).getMatchingsWithFilters(
                    MatchingStatus.OPEN, null, null, null, null, null, pageable);
        }
    }

    @Nested
    @DisplayName("관리자 매칭 거절 테스트")
    class RejectMatchingTest {

        @Test
        @DisplayName("매칭 거절 성공 - 대기 사용자들이 있는 경우")
        void rejectMatchingWithWaitingUsers() {
            // given
            List<WaitingUser> waitingUsers = Arrays.asList(waitingUser1, waitingUser2);

            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);

            // when
            adminMatchingService.rejectMatching(1L);

            // then
            verify(matchingService).findMatchingById(1L);
            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);
            verify(waitingUser1).reject();
            verify(waitingUser2).reject();
            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching).rejectMatching();

            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
            verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
            verify(redisMatchingService).decrementUserActiveMatchingCount(3L);
        }

        @Test
        @DisplayName("매칭 거절 성공 - 대기 사용자가 없는 경우")
        void rejectMatchingWithoutWaitingUsers() {
            // given
            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(Collections.emptyList());

            // when
            adminMatchingService.rejectMatching(1L);

            // then
            verify(matchingService).findMatchingById(1L);
            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);
            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching).rejectMatching();

            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
        }

        @Test
        @DisplayName("매칭 거절 성공 - 일부 대기 사용자는 이미 처리된 상태")
        void rejectMatchingWithMixedWaitingUserStatuses() {
            // given
            WaitingUser acceptedWaitingUser = mock(WaitingUser.class);
            when(acceptedWaitingUser.getId()).thenReturn(3L);
            when(acceptedWaitingUser.getStatus()).thenReturn(WaitingStatus.ACCEPTED);
            when(acceptedWaitingUser.getWaitingUser()).thenReturn(applicant2);

            WaitingUser rejectedWaitingUser = mock(WaitingUser.class);
            when(rejectedWaitingUser.getId()).thenReturn(4L);
            when(rejectedWaitingUser.getStatus()).thenReturn(WaitingStatus.REJECTED);
            when(rejectedWaitingUser.getWaitingUser()).thenReturn(applicant1);

            List<WaitingUser> waitingUsers = Arrays.asList(
                    waitingUser1,
                    acceptedWaitingUser,
                    rejectedWaitingUser
            );

            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(waitingUsers);

            // when
            adminMatchingService.rejectMatching(1L);

            // then
            verify(matchingService).findMatchingById(1L);
            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);

            verify(waitingUser1).reject();
            verify(acceptedWaitingUser, never()).reject();
            verify(rejectedWaitingUser, never()).reject();

            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching).rejectMatching();

            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
            verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
        }

        @Test
        @DisplayName("이미 닫힌 매칭 거절 시 예외 발생")
        void rejectAlreadyClosedMatching() {
            // given
            when(matching.isOpen()).thenReturn(false);
            given(matchingService.findMatchingById(1L)).willReturn(matching);

            // when & then
            assertThatThrownBy(() -> adminMatchingService.rejectMatching(1L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_ALREADY_CLOSED);

            verify(matchingService).findMatchingById(1L);
            verify(waitingUserRepository, never()).findByMatchingOrderByCreatedAtDesc(any(Matching.class));
            verify(chatRoomService, never()).deleteChatRoom(any(ChatRoom.class));
            verify(matching, never()).rejectMatching();
        }

        @Test
        @DisplayName("존재하지 않는 매칭 거절 시 예외 발생")
        void rejectNonExistentMatching() {
            // given
            given(matchingService.findMatchingById(999L))
                    .willThrow(new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> adminMatchingService.rejectMatching(999L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MatchingErrorCode.MATCHING_NOT_FOUND);

            verify(matchingService).findMatchingById(999L);
            verify(waitingUserRepository, never()).findByMatchingOrderByCreatedAtDesc(any(Matching.class));
            verify(chatRoomService, never()).deleteChatRoom(any(ChatRoom.class));
        }

        @Test
        @DisplayName("채팅룸이 null인 매칭 거절")
        void rejectMatchingWithNullChatRoom() {
            // given
            when(matching.getChatRoom()).thenReturn(null);
            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(Collections.emptyList());

            // when
            adminMatchingService.rejectMatching(1L);

            // then
            verify(matchingService).findMatchingById(1L);
            verify(chatRoomService).deleteChatRoom(null);
            verify(matching).rejectMatching();
            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
        }

        @Test
        @DisplayName("다수의 대기 사용자가 있는 매칭 거절")
        void rejectMatchingWithManyWaitingUsers() {
            // given
            List<WaitingUser> manyWaitingUsers = Arrays.asList(
                    createMockWaitingUser(1L, 2L),
                    createMockWaitingUser(2L, 3L),
                    createMockWaitingUser(3L, 4L),
                    createMockWaitingUser(4L, 5L),
                    createMockWaitingUser(5L, 6L)
            );

            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(manyWaitingUsers);

            // when
            adminMatchingService.rejectMatching(1L);

            // then
            verify(matchingService).findMatchingById(1L);
            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);

            manyWaitingUsers.forEach(waitingUser -> verify(waitingUser).reject());

            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching).rejectMatching();

            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService, times(6)).decrementUserActiveMatchingCount(anyLong());
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
        }

        private WaitingUser createMockWaitingUser(Long waitingUserId, Long userId) {
            WaitingUser waitingUser = mock(WaitingUser.class);
            User user = mock(User.class);

            when(user.getId()).thenReturn(userId);
            when(waitingUser.getId()).thenReturn(waitingUserId);
            when(waitingUser.getWaitingUser()).thenReturn(user);
            when(waitingUser.getStatus()).thenReturn(WaitingStatus.PENDING);

            return waitingUser;
        }
    }

    @Nested
    @DisplayName("관리자 권한 및 예외 상황 테스트")
    class AdminAuthorityAndExceptionTest {

        @Test
        @DisplayName("매칭 서비스 의존성 예외 처리")
        void handleMatchingServiceDependencyException() {
            // given
            given(matchingService.findMatchingById(1L))
                    .willThrow(new RuntimeException("Database connection error"));

            // when & then
            assertThatThrownBy(() -> adminMatchingService.rejectMatching(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");

            verify(matchingService).findMatchingById(1L);
            verify(waitingUserRepository, never()).findByMatchingOrderByCreatedAtDesc(any(Matching.class));
        }

        @Test
        @DisplayName("채팅룸 서비스 의존성 예외 처리")
        void handleChatRoomServiceDependencyException() {
            // given
            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(Collections.emptyList());
            doThrow(new RuntimeException("Chat room deletion failed"))
                    .when(chatRoomService).deleteChatRoom(chatRoom);

            // when & then
            assertThatThrownBy(() -> adminMatchingService.rejectMatching(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Chat room deletion failed");

            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching, never()).rejectMatching();
        }

        @Test
        @DisplayName("트랜잭션 후 Redis 작업 실행 확인")
        void verifyRedisOperationsAfterTransaction() {
            // given
            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willReturn(Collections.emptyList());

            // when
            adminMatchingService.rejectMatching(1L);

            // then
            verify(chatRoomService).deleteChatRoom(chatRoom);
            verify(matching).rejectMatching();

            verify(redisMatchingService).decrementUserActiveMatchingCount(1L);
            verify(redisMatchingService).removeMatchingFromAvailableSet(1L, InitiatorType.SPEAKER);
            verify(redisMatchingService).cleanupMatchingKeys(matching);
        }

        @Test
        @DisplayName("대기 사용자 repository 예외 처리")
        void handleWaitingUserRepositoryException() {
            // given
            given(matchingService.findMatchingById(1L)).willReturn(matching);
            given(waitingUserRepository.findByMatchingOrderByCreatedAtDesc(matching))
                    .willThrow(new RuntimeException("Database query failed"));

            // when & then
            assertThatThrownBy(() -> adminMatchingService.rejectMatching(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database query failed");

            verify(waitingUserRepository).findByMatchingOrderByCreatedAtDesc(matching);
            verify(chatRoomService, never()).deleteChatRoom(any(ChatRoom.class));
            verify(matching, never()).rejectMatching();
        }
    }
}