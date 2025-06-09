package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageEncryptConsumer {
    private final AesGcmEncryptionService aesGcmEncryptionService;
    private final ChatMessageService chatMessageService;

//    // 실패 확률 (0.0 ~ 1.0)
//    private static final double FAILURE_PROBABILITY = 0.7; // 70% 확률로 실패
//
//    // 랜덤 생성기
//    private final Random random = new Random();

//    @KafkaStandardRetry
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0), // 지수 백오프 적용
            dltTopicSuffix = "-message-encryption-group-dlt",
            retryTopicSuffix = "-message-encryption-group-retry",
            autoCreateTopics = "false"
    )
    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "message-encryption-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    @Transactional
    public void encryptMessage(ConsumerRecord<String, ChatMessageEvent> record, Acknowledgment ack) {
        ChatMessageEvent event = record.value();

        if (event.isFiltered() || event.isEncrypted() || event.getMessageId() == null) {
            ack.acknowledge();
            return;
        }

//        // 테스트 오류 시뮬레이션
//        if (event.getContent() != null && event.getContent().contains("error")) {
//            // 랜덤 확률로 실패 발생
//            if (random.nextDouble() < FAILURE_PROBABILITY) {
//                log.error("테스트 예외 발생 ({}% 확률): messageId={}",
//                        (int)(FAILURE_PROBABILITY * 100), event.getMessageId());
//                throw new RuntimeException("의도적으로 발생시킨 테스트 예외");
//            } else {
//                log.info("운이 좋아 이번에는 성공 처리 ({}% 확률로 실패): messageId={}",
//                        (int)(FAILURE_PROBABILITY * 100), event.getMessageId());
//            }
//        }

        try {
            ChatMessage message = chatMessageService.findChatMessageById(event.getMessageId());
            String encryptedContent = aesGcmEncryptionService.encrypt(message.getContent());
            message.updateEncryptedContent(encryptedContent);
            chatMessageService.save(message);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("메시지 암호화 중 오류: {}", e.getMessage(), e);
            throw e;
        }
    }
}
