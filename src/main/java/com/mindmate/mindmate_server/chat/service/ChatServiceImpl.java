package com.mindmate.mindmate_server.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final ChatRoomService chatRoomService;
    private final UserService userService;
//    private final ChatMessageService chatMessageService;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getRoomId());
        User user = userService.findUserById(userId);

        ChatMessageEvent event = ChatMessageEvent.builder()
                .roomId(chatRoom.getId())
                .senderId(userId)
                .senderRole(user.getCurrentRole())
                .content(request.getContent())
                .type(request.getType())
                .timestamp(LocalDateTime.now())
                .build();

        // todo : redis 채널에 메시지 발행?
        String channel = "chat:room:" + chatRoom.getId();
        ChatMessageResponse chatMessageResponse = ChatMessageResponse.builder()
                .senderId(userId)
                .senderRole(user.getCurrentRole())
                .content(request.getContent())
                .type(request.getType())
                .createdAt(LocalDateTime.now())
                .roomId(chatRoom.getId())
                .build();
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.error("Error serializing message", e);
        }

        // 채팅 메시지의 저장이 비동기로 처리되므로 해당 id를 알 수가 없음 -> roomid를 파티션 키로 사용하여 동일한 채팅방의 메시지는 항상 같은 파티션으로 전달
        kafkaTemplate.send("chat-message-topic", event.getRoomId().toString(), event);
        log.info("Chat message sent to Kafka: {}", event);
        return chatMessageResponse;
    }

    @Override
    public int markAsRead(Long userId, Long roomId) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(roomId);
        User user = userService.findUserById(userId);

        validateChatRoomAccess(chatRoom, user);
        boolean isListener = isUserListener(chatRoom, user);
        Long lastMessageId = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)
                .map(ChatMessage::getId)
                .orElse(0L);

        if (isListener) {
            chatRoom.markAsReadForListener(lastMessageId);
        } else {
            chatRoom.markAsReadForSpeaker(lastMessageId);
        }

        String unreadKey = "chat:room:" + roomId + ":unread:" + userId;
        redisTemplate.delete(unreadKey);


        String key = "chat:room:" + roomId + ":read:" + userId;
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());

        Map<String, Object> readEvent = new HashMap<>();
        readEvent.put("type", "READ_STATUS");
        readEvent.put("roomId", roomId);
        readEvent.put("userId", userId);
        readEvent.put("timestamp", LocalDateTime.now());

        String channel = "chat:room:" + roomId;
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(readEvent));
        } catch (JsonProcessingException e) {
            log.error("Error serializing read event", e);
        }
        return 0;
    }

    private void validateChatRoomAccess(ChatRoom chatRoom, User user) {
        boolean hasAccess = chatRoom.getListener().getUser().getId().equals(user.getId()) ||
                chatRoom.getSpeaker().getUser().getId().equals(user.getId());

        if (!hasAccess) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    private boolean isUserListener(ChatRoom chatRoom, User user) {
        return chatRoom.getListener().getUser().equals(user);
    }

//    @Override
//    public void updateUserStatus(Long userId, boolean isOnline, Long activeRoomId) {
//        String statusKey = "user:status:" + userId;
//        Map<String, Object> status = new HashMap<>();
//        status.put("online", isOnline);
//        status.put("activeRoomId", activeRoomId);
//        status.put("lastActive", LocalDateTime.now());
//
//        redisTemplate.opsForHash().putAll(statusKey, status);
//
//        if (isOnline) {
//            redisTemplate.expire(statusKey, 30, TimeUnit.MINUTES);
//        }
//
//        String channel = "user:status:" + userId;
//        redisTemplate.convertAndSend(channel, status);
//    }
}
