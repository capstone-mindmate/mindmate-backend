package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.WaitingStatus;
import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingEventConsumerTest {

    @Mock
    private WaitingUserRepository waitingUserRepository;
    @Mock
    private RedisMatchingService redisMatchingService;
    @Mock
    private Acknowledgment acknowledgment;
    @InjectMocks
    private MatchingEventConsumer matchingEventConsumer;

    @Mock
    private User creator;
    @Mock
    private User applicant1;
    @Mock
    private User applicant2;
    @Mock
    private Matching matching;
    @Mock
    private WaitingUser waitingUser1;
    @Mock
    private WaitingUser waitingUser2;

    @BeforeEach
    void setUp() {
        when(creator.getId()).thenReturn(1L);
        when(applicant1.getId()).thenReturn(2L);
        when(applicant2.getId()).thenReturn(3L);

        when(matching.getId()).thenReturn(1L);
        when(matching.getCreator()).thenReturn(creator);

        when(waitingUser1.getId()).thenReturn(1L);
        when(waitingUser1.getWaitingUser()).thenReturn(applicant1);
        when(waitingUser1.getMatching()).thenReturn(matching);
        when(waitingUser1.getStatus()).thenReturn(WaitingStatus.PENDING);

        when(waitingUser2.getId()).thenReturn(2L);
        when(waitingUser2.getWaitingUser()).thenReturn(applicant2);
        when(waitingUser2.getMatching()).thenReturn(matching);
        when(waitingUser2.getStatus()).thenReturn(WaitingStatus.PENDING);
    }

    @Test
    @DisplayName("매칭 수락 이벤트 처리")
    void handleMatchingAccepted() {
        // given
        MatchingAcceptedEvent event = MatchingAcceptedEvent.builder()
                .matchingId(1L)
                .creatorId(1L)
                .acceptedUserId(2L)
                .pendingWaitingUserIds(Arrays.asList(1L, 2L))
                .build();

        when(waitingUserRepository.findById(1L)).thenReturn(Optional.of(waitingUser1));
        when(waitingUserRepository.findById(2L)).thenReturn(Optional.of(waitingUser2));

        // when
        matchingEventConsumer.handleMatchingAccepted(event, acknowledgment);

        // then
        verify(waitingUserRepository, times(2)).findById(anyLong());
        verify(waitingUser1).reject();
        verify(waitingUser2).reject();
        verify(waitingUserRepository).save(waitingUser1);
        verify(waitingUserRepository).save(waitingUser2);
        verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
        verify(redisMatchingService).decrementUserActiveMatchingCount(3L);
    }

    @Test
    @DisplayName("매칭 수락 이벤트 처리 - 대기 사용자 없음")
    void acceptNoWaitingUser() {
        // given
        MatchingAcceptedEvent event = MatchingAcceptedEvent.builder()
                .matchingId(1L)
                .creatorId(1L)
                .acceptedUserId(2L)
                .pendingWaitingUserIds(Arrays.asList(999L))
                .build();

        when(waitingUserRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        matchingEventConsumer.handleMatchingAccepted(event, acknowledgment);

        // then
        verify(waitingUserRepository).findById(999L);
        verify(waitingUserRepository, never()).save(any(WaitingUser.class));
        verify(redisMatchingService, never()).decrementUserActiveMatchingCount(anyLong());
    }

    @Test
    @DisplayName("매칭 수락 이벤트 처리 - 빈 대기 사용자 목록")
    void acceptEmptyWaitingUserList() {
        // given
        MatchingAcceptedEvent event = MatchingAcceptedEvent.builder()
                .matchingId(1L)
                .creatorId(1L)
                .acceptedUserId(2L)
                .pendingWaitingUserIds(Arrays.asList())
                .build();

        // when
        matchingEventConsumer.handleMatchingAccepted(event, acknowledgment);

        // then
        verify(waitingUserRepository, never()).findById(anyLong());
        verify(waitingUserRepository, never()).save(any(WaitingUser.class));
        verify(redisMatchingService, never()).decrementUserActiveMatchingCount(anyLong());
    }

    @Test
    @DisplayName("매칭 수락 이벤트 처리 - 예외 발생")
    void handleMatchingAcceptedException() {
        // given
        MatchingAcceptedEvent event = MatchingAcceptedEvent.builder()
                .matchingId(1L)
                .creatorId(1L)
                .acceptedUserId(2L)
                .pendingWaitingUserIds(Arrays.asList(1L, 2L))
                .build();

        when(waitingUserRepository.findById(1L)).thenReturn(Optional.of(waitingUser1));
        when(waitingUserRepository.findById(2L)).thenThrow(new RuntimeException("Database error"));

        // when
        assertThrows(RuntimeException.class, () ->
                matchingEventConsumer.handleMatchingAccepted(event, acknowledgment));
        // then
        verify(waitingUserRepository, times(2)).findById(anyLong());
        verify(waitingUser1).reject();
        verify(waitingUserRepository).save(waitingUser1);
        verify(redisMatchingService).decrementUserActiveMatchingCount(2L);
    }
}