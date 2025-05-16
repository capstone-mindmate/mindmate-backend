package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.util.KafkaStandardRetry;
import com.mindmate.mindmate_server.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResponseTimeCalculationConsumer {
    private final ProfileService profileService;

    private final ChatMessageRepository chatMessageRepository;

//    @KafkaStandardRetry
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000),
            dltTopicSuffix = "-response-time-calculation-group-dlt",
            retryTopicSuffix = "-response-time-calculation-group-retry"
    )
    @KafkaListener(
            topics = "chat-room-close-topic",
            groupId = "response-time-calculation-group",
            containerFactory = "chatRoomCloseListenerContainerFactory"
    )
    @Transactional
    public void calculateResponseTime(ConsumerRecord<String, ChatRoomCloseEvent> record, Acknowledgment ack) {
        ChatRoomCloseEvent event = record.value();

        try {
            Map<Long, List<Integer>> userResponseTimes = new HashMap<>();
            userResponseTimes.put(event.getSpeakerId(), new ArrayList<>());
            userResponseTimes.put(event.getListenerId(), new ArrayList<>());

            int batchSize = 500;
            Long lastMessageId = null;
            boolean hasMore = true;
            ChatMessage previousMessage = null;

            while (hasMore) {
                List<ChatMessage> messages;

                if (lastMessageId == null) {
                    // 첫 번째 배치는 처음부터 batchSize만큼 조회
                    messages = chatMessageRepository.findByChatRoomIdOrderByIdAsc(
                            event.getChatRoomId(),
                            PageRequest.of(0, batchSize)
                    );
                } else {
                    messages = chatMessageRepository.findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                            event.getChatRoomId(),
                            lastMessageId,
                            PageRequest.of(0, batchSize)
                    );
                }

                if (messages.isEmpty()) {
                    hasMore = false;
                    continue;
                }

                // 이전 배치 마지막 메시지 - 현재 배치 첫 메시지 사이의 시간 계산
                if (previousMessage != null) {
                    processResponseTime(previousMessage, messages.get(0), userResponseTimes);
                }

                for (int i = 0; i < messages.size() - 1; i++) {
                    processResponseTime(messages.get(i), messages.get(i + 1), userResponseTimes);
                }

                lastMessageId = messages.get(messages.size() - 1).getId();
                previousMessage = messages.get(messages.size() - 1);

                hasMore = messages.size() == batchSize;
            }

            updateUserResponseTimes(event.getSpeakerId(), userResponseTimes.get(event.getSpeakerId()));
            updateUserResponseTimes(event.getListenerId(), userResponseTimes.get(event.getListenerId()));
            profileService.incrementCounselingCount(event.getSpeakerId());
            profileService.incrementCounselingCount(event.getListenerId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error calculating response times: {}", e.getMessage(), e);
            throw e;
        }


    }

    private void processResponseTime(ChatMessage previous, ChatMessage current, Map<Long, List<Integer>> userResponseTimes) {
        // 발신자가 다른 경우만 처리
        if (!previous.getSender().getId().equals(current.getSender().getId())) {
            Duration responseTime = Duration.between(
                    previous.getCreatedAt(),
                    current.getCreatedAt()
            );
            int responseTimeMinutes = (int) responseTime.toMinutes();

            // 합리적인 응답 시간만 계산
            if (responseTimeMinutes >= 0 && responseTimeMinutes < 24 * 60) {
                userResponseTimes.get(current.getSender().getId()).add(responseTimeMinutes);
            }
        }
    }

    private void updateUserResponseTimes(Long userId, List<Integer> responseTimes) {
        if (responseTimes.isEmpty()) {
            return;
        }

        profileService.updateResponseTimes(userId, responseTimes);
    }
}
