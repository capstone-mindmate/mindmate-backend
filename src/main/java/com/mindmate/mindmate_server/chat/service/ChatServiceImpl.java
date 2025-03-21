package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.domain.FilteringWordCategory;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisKeyManager redisKeyManager;

    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final ChatPresenceService chatPresenceService;
    private final ContentFilterService contentFilterService;
    private final ChatMessageService chatMessageService;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    // todo: 채팅 관련 전체적으로 채팅방 상태에 따른 처리 추가해야함. 메시지 보내기 +

    /**
     * 필터링 + 메시지 저장 동기적 처리
     * 1. 읽음 안읽음 처리
     * 2. 알림 서비스
     * 3. 필터링 기반 토스트 박스
     */
    @Override
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        try {
            ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getRoomId());
            User sender = userService.findUserById(userId);

            chatRoomService.validateChatActivity(sender, chatRoom);

            // 필터링 동기 처리
            Optional<FilteringWordCategory> filteringCategory =
                    contentFilterService.findFilteringWordCategory(request.getContent());

            if (filteringCategory.isPresent()) {
                return handleFilteredMessage(filteringCategory.get(), chatRoom, sender, request);
            }

            return handleNormalMessage(chatRoom, sender, request);
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생", e);

            return ChatMessageResponse.errorResponse(
                    request.getRoomId(),
                    userId,
                    "알 수 없음",
                    request.getContent(),
                    request.getType(),
                    "메시지 전송에 실패했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    @Override
//    @Transactional(propagation = Propagation.REQUIRED)
    public int markAsRead(Long userId, Long roomId) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(roomId);
        User user = userService.findUserById(userId);

        validateChatRoomAccess(chatRoom, user);
        Long lastMessageId = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(0L);

        chatRoom.markAsRead(user, lastMessageId);
        chatRoomRepository.saveAndFlush(chatRoom);

        chatPresenceService.resetUnreadCount(roomId, userId);
        String key = redisKeyManager.getReadStatusKey(roomId, userId);
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
        redisTemplate.expire(key, 1, TimeUnit.DAYS);

        Map<String, Object> readEvent = new HashMap<>();
        readEvent.put("type", "READ_STATUS");
        readEvent.put("roomId", roomId);
        readEvent.put("userId", userId);
        readEvent.put("timestamp", LocalDateTime.now());

        String channel = redisKeyManager.getChatRoomChannel(roomId);
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(readEvent));
        } catch (JsonProcessingException e) {
            log.error("Error serializing read event", e);
        }
        return 0;
    }

    private ChatMessageResponse handleNormalMessage(ChatRoom chatRoom, User sender, ChatMessageRequest request) {
        // 필터링 통과 시 저장 후 비동기 처리 시작
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        chatRoom.updateLastMessageTime();

        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(savedMessage.getId())
                .roomId(chatRoom.getId())
                .senderId(sender.getId())
                .content(request.getContent())
                .type(request.getType())
                .timestamp(savedMessage.getCreatedAt())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("chat-message-topic", event.getRoomId().toString(), event);
            }
        });

        ChatMessageResponse chatMessageResponse = ChatMessageResponse.from(savedMessage, sender.getId());

//        String channel = redisKeyManager.getChatRoomChannel(chatRoom.getId());
//        try {
//             Redis의 Pub/Sub 기능을 사용하여 지정한 채널에 메시지를 publish
//            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
//        } catch (JsonProcessingException e) {
//            log.error("Error serializing message", e);
//        }

        return chatMessageResponse;
    }

    private ChatMessageResponse handleFilteredMessage(FilteringWordCategory filteringWordCategory, ChatRoom chatRoom, User sender, ChatMessageRequest request) {
        String filteredContent = String.format(
                "[%s 관련 부적절한 내용이 감지되었습니다]",
                filteringWordCategory.getDescription());
//        publishFilterEvent(chatRoom.getId(), sender.getId(), filteredContent);

        return ChatMessageResponse.filteredResponse(
                chatRoom.getId(),
                sender.getId(),
                sender.getProfile().getNickname(),
                filteredContent,
                request.getType()
        );
    }

    /**
     * todo: 굳이 redis 통해 이벤트 발행해야 하나??
     * 사용하는 이유가 없는 것 같은데
     * 이벤트 기반 아키텍처나 실시간 알림의 이유?
     */
    private void publishFilterEvent(Long roomId, Long senderId, String filteredContent) {
        // 필터링 결과 알림 이벤트 발행 (Redis)
        Map<String, Object> filterEvent = new HashMap<>();
        filterEvent.put("type", "CONTENT_FILTERED");
        filterEvent.put("roomId", roomId);
        filterEvent.put("senderId", senderId);
        filterEvent.put("content", filteredContent);
        filterEvent.put("timestamp", LocalDateTime.now());

        String channel = redisKeyManager.getChatRoomChannel(roomId);
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(filterEvent));
        } catch (JsonProcessingException e) {
            log.error("Error serializing filter event", e);
        }
    }

    private void validateChatRoomAccess(ChatRoom chatRoom, User user) {
        boolean hasAccess = chatRoom.getListener().getId().equals(user.getId()) ||
                chatRoom.getSpeaker().getId().equals(user.getId());
        log.info("listener id: {} / speaker id: {}", chatRoom.getListener().getId(), chatRoom.getSpeaker().getId());

        if (!hasAccess) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }
}
