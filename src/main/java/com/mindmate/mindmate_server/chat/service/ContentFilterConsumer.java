package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
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
    private final ChatMessageRepository chatMessageRepository;

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

        try {
            ChatMessage message = chatMessageService.findChatMessageById(event.getMessageId());
            Optional<FilteringWordCategory> filteringCategory = contentFilterService.findFilteringWordCategory(message.getContent());

            if (filteringCategory.isPresent()) {
                log.warn("Banned word detected in message: {}, category: {}",
                        event.getMessageId(), filteringCategory.get().getDescription());

                String filteredContent = String.format(
                        "[%s 관련 부적절한 내용이 감지되었습니다]",
                        filteringCategory.get().getDescription());
                message.setFilteredContent(filteredContent);
                chatMessageRepository.save(message); // 명시적 저장 추가

                String channel = "chat:room:" + message.getChatRoom().getId();
                try {
                    Map<String, Object> filterEvent = new HashMap<>();
                    filterEvent.put("type", "CONTENT_FILTERED");
                    filterEvent.put("messageId", message.getId());
                    filterEvent.put("content", filteredContent);

                    // Pub/Sub으로 필터링 이벤트 발행 -> 클라이언트에서 처리
                    redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(filterEvent));
                } catch (JsonProcessingException e) {
                    log.error("Error serializing filter event", e);
                }
            }
        } catch (Exception e) {
            log.error("Error filtering content", e);
        }
    }
}
