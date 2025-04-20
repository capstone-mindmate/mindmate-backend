package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import com.mindmate.mindmate_server.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
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
    private final ChatMessageService chatMessageService;
    private final ProfileService profileService;

    @KafkaListener(
            topics = "chat-room-close-topic",
            groupId = "response-time-calculation-group",
            containerFactory = "chatRoomCloseListenerContainerFactory"
    )
    @Transactional
    public void calculateResponseTime(ConsumerRecord<String, ChatRoomCloseEvent> record) {
        ChatRoomCloseEvent event = record.value();

        try {
            List<ChatMessage> messages = chatMessageService.findAllByChatRoomIdOrderByIdAsc(event.getChatRoomId());
            Map<Long, List<Integer>> userResponseTimes = new HashMap<>();
            userResponseTimes.put(event.getSpeakerId(), new ArrayList<>());
            userResponseTimes.put(event.getListenerId(), new ArrayList<>());

            ChatMessage previousMessage = null;
            for (ChatMessage message : messages) {
                // 이전 메시지 - 현재 메시지 발신자가 다를 경우
                if (previousMessage != null && !previousMessage.getSender().getId().equals(message.getSender().getId())) {
                    Duration responseTime = Duration.between(
                            previousMessage.getCreatedAt(),
                            message.getCreatedAt()
                    );
                    int responseTimeMinutes = (int) responseTime.toMinutes();

                    // 합리적인 응답 시간만 계산 -> 막 2, 3일 뒤에 응답한 것까지 하기에는 아닌 것 같다
                    if (responseTimeMinutes >= 0 && responseTimeMinutes < 24 * 60) {
                        userResponseTimes.get(message.getSender().getId()).add(responseTimeMinutes);
                    }
                }
                previousMessage = message;
            }

            updateUserResponseTimes(event.getSpeakerId(), userResponseTimes.get(event.getSpeakerId()));
            updateUserResponseTimes(event.getListenerId(), userResponseTimes.get(event.getListenerId()));
        } catch (Exception e) {
            log.error("Error calculating response times: {}", e.getMessage(), e);
        }


    }

    private void updateUserResponseTimes(Long userId, List<Integer> responseTimes) {
        if (responseTimes.isEmpty()) {
            return;
        }

        profileService.updateResponseTimes(userId, responseTimes);
    }
}
