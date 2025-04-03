package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchingEventProducerTest {

    @Mock
    private KafkaTemplate<String, MatchingAcceptedEvent> kafkaTemplate;

    @InjectMocks
    private MatchingEventProducer matchingEventProducer;

    @Test
    @DisplayName("매칭 수락 이벤트 발행")
    void publishMatchingAccepted() {
        // given
        MatchingAcceptedEvent event = MatchingAcceptedEvent.builder()
                .matchingId(1L)
                .creatorId(2L)
                .acceptedUserId(3L)
                .pendingWaitingUserIds(Arrays.asList(4L, 5L, 6L))
                .build();

        // when
        matchingEventProducer.publishMatchingAccepted(event);

        // then
        verify(kafkaTemplate).send(eq("matching-accepted"), eq("1"), eq(event));
    }
}