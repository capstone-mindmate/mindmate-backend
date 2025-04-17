package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MessageEncryptConsumer {
    private final AesGcmEncryptionService aesGcmEncryptionService;
    private final ChatMessageService chatMessageService;

    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "message-encryption-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void encryptMessage(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();

        if (event.isFiltered() || event.isEncrypted() || event.getMessageId() == null) {
            return;
        }

        try {
            ChatMessage message = chatMessageService.findChatMessageById(event.getMessageId());

            String encryptedContent = aesGcmEncryptionService.encrypt(message.getContent());

            message.updateEncryptedContent(encryptedContent);
            chatMessageService.save(message);

        } catch (Exception e) {
            log.error("메시지 암호화 중 오류: {}", e.getMessage(), e);
        }
    }
}
