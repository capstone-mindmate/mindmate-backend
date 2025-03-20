package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatMessage findChatMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.MESSAGE_NOT_FOUND));
    }

    @Override
    public long countMessagesByChatRoomId(Long roomId) {
        return chatMessageRepository.countByChatRoomId(roomId);
    }

    @Override
    public List<ChatMessage> findByChatRoomIdOrderByIdAsc(Long roomId, int size) {
        long totalMessages = countMessagesByChatRoomId(roomId);
        return new ArrayList<>(chatMessageRepository.findByChatRoomIdOrderByIdAsc(
                roomId, PageRequest.of(0, (int)Math.min(totalMessages, size))).getContent());
    }

    @Override
    public Optional<ChatMessage> findLatestMessageByChatRoomId(Long roomId) {
        return chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId);
    }

    @Override
    public List<ChatMessage> findMessagesBeforeId(Long roomId, Long messageId, int size) {
        return chatMessageRepository.findMessagesBeforeIdLimited(roomId, messageId, PageRequest.of(0, size));
    }

    @Override
    public List<ChatMessage> findMessagesAfterOrEqualId(Long roomId, Long messageId) {
        return chatMessageRepository.findByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(roomId, messageId);
    }

    @Override
    public List<ChatMessage> findRecentMessages(Long roomId, int size) {
        List<ChatMessage> tempMessages = chatMessageRepository
                .findByChatRoomIdOrderByIdDesc(roomId, PageRequest.of(0, size)).getContent();
        List<ChatMessage> messages = new ArrayList<>(tempMessages);
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public List<ChatMessage> findPreviousMessages(Long roomId, Long messageId, int size) {
        List<ChatMessage> tempMessages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(
                roomId, messageId, PageRequest.of(0, size)).getContent();
        List<ChatMessage> messages = new ArrayList<>(tempMessages);
        Collections.reverse(messages);
        return messages;
    }

}
