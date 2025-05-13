package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageEncryptConsumer {
    private final AesGcmEncryptionService aesGcmEncryptionService;
    private final ChatMessageService chatMessageService;

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

//        if (event.getContent() != null && event.getContent().contains("error")) {
//            log.error("테스트 예외 발생: {}", event.getMessageId());
//            throw new RuntimeException("의도적으로 발생시킨 테스트 예외");
//        }


        try {
            ChatMessage message = chatMessageService.findChatMessageById(event.getMessageId());

            String encryptedContent = aesGcmEncryptionService.encrypt(message.getContent());

            message.updateEncryptedContent(encryptedContent);
            chatMessageService.save(message);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("메시지 암호화 중 오류: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
