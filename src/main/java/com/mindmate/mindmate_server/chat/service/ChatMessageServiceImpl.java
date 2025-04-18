package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final AesGcmEncryptionService encryptionService;

    @Override
    public ChatMessage findChatMessageById(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.MESSAGE_NOT_FOUND));

        // 암호화된 메시지 복호화
        decryptMessageIfNeeded(message);
        return message;
    }

    @Override
    public long countMessagesByChatRoomId(Long roomId) {
        return chatMessageRepository.countByChatRoomId(roomId);
    }

    @Override
    public List<ChatMessage> findAllByChatRoomIdOrderByIdAsc(Long roomId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByIdAsc(roomId);
        return decryptMessagesIfNeeded(messages);
    }

    @Override
    public Optional<ChatMessage> findLatestMessageByChatRoomId(Long roomId) {
        return chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId);
    }

    @Override
    public List<ChatMessage> findMessagesBeforeId(Long roomId, Long messageId, int size) {
        List<ChatMessage> messages = chatMessageRepository.findMessagesBeforeIdLimited(roomId, messageId, PageRequest.of(0, size));
        return decryptMessagesIfNeeded(messages);
    }
    @Override
    public List<ChatMessage> findMessagesAfterOrEqualId(Long roomId, Long messageId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(roomId, messageId);
        return decryptMessagesIfNeeded(messages);
    }

    @Override
    public List<ChatMessage> findRecentMessages(Long roomId, int size) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByIdDesc(roomId, PageRequest.of(0, size)).getContent();
        List<ChatMessage> decryptedMessages = decryptMessagesIfNeeded(messages);

        List<ChatMessage> reversedMessages = new ArrayList<>(decryptedMessages);
        Collections.reverse(reversedMessages);

        return reversedMessages;
    }

    @Override
    public List<ChatMessage> findPreviousMessages(Long roomId, Long messageId, int size) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, messageId, PageRequest.of(0, size)).getContent();
        List<ChatMessage> decryptedMessages = decryptMessagesIfNeeded(messages);

        List<ChatMessage> reversedMessages = new ArrayList<>(decryptedMessages);
        Collections.reverse(reversedMessages);

        return reversedMessages;
    }

    @Override
    @Transactional
    public ChatMessage save(ChatMessage chatMessage) {
        return chatMessageRepository.save(chatMessage);
    }

    @Override
    public List<Long> findMessageIdsByKeyword(Long roomId, String keyword) {
        if (roomId == null || keyword == null || keyword.trim().isEmpty()) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_INVALID_PARAMETERS);
        }
        return chatMessageRepository.findMessageIdsByKeyword(roomId, keyword);
    }

    @Override
    public List<ChatMessage> findByRoomIdAndIdBetween(Long roomId, Long targetMessageId, Long lastMessageId) {
        return chatMessageRepository.findByRoomIdAndIdBetween(roomId, targetMessageId, lastMessageId);
    }

    // 단일 메시지 확인 시 보고화
    private void decryptMessageIfNeeded(ChatMessage message) {
        if (message.isEncrypted()) {
            try {
                String decryptedContent = encryptionService.decrypt(message.getContent());
                message.setDecryptedContent(decryptedContent);
            } catch (Exception e) {
            }
        }
    }

    // 메서지 목록들 확인 시 복호화
    private List<ChatMessage> decryptMessagesIfNeeded(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            decryptMessageIfNeeded(message);
        }
        return messages;
    }
}
