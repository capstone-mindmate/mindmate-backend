package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ContentFilterConsumer {
    private final ContentFilterService contentFilterService;
    private final ChatMessageService chatMessageService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "content-filter-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void filterContent(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();
        log.info("Filtering content for message: {}", event);

        // todo : 해당 id 설정 독립적 동작 어떻게?
//        if (event.getMessageId() == null) {
//            log.warn("Message ID not set, waiting for storage consumer to process");
//            return;
//        }

        try {
            Optional<FilteringWordCategory> filteringCategory = contentFilterService.findFilteringWordCategory(event.getContent());

            if (filteringCategory.isPresent()) {
                log.warn("Banned word detected in message: {}, category: {}",
                        event.getMessageId(), filteringCategory.get().getDescription());

                ChatMessage message = chatMessageService.findChatMessageById(event.getMessageId());

                // todo : 필터링 처리 어떻게 할 것인지? + 그리고 필터링 후 저장을 원본으로? 아니면 수정해서/
                // option 1: 아예 다른 내용으로 대체
                // option 2: 해당 단어만 마스킹
                if (message != null) {
                    String warningMessage = String.format(
                            "[%s 관련 부적절한 내용이 감지되어 삭제되었습니다]",
                            filteringCategory.get().getDescription());

                    String channel = "chat:room:" + message.getChatRoom().getId();
                    try {
                        Map<String, Object> filterEvent = new HashMap<>();
                        filterEvent.put("type", "CONTENT_FILTERED");
                        filterEvent.put("messageId", message.getId());
                        filterEvent.put("content", message.getContent());

                        redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(filterEvent));
                    } catch (JsonProcessingException e) {
                        log.error("Error serializing filter event", e);
                    }

                }
            }
        } catch (Exception e) {
            log.error("Error filtering content", e);
        }
    }
}
