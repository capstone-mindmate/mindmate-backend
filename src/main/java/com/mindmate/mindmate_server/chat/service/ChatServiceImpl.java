package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.global.service.ResilientEventPublisher;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyManager redisKeyManager;
    private final ResilientEventPublisher eventPublisher;

    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final ChatPresenceService chatPresenceService;
    private final ContentFilterService contentFilterService;
    private final ChatMessageService chatMessageService;
    /**
     * 필터링 + 메시지 저장 동기적 처리 이후 비동기 처리
     */
    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
        try {
            ChatRoom chatRoom = chatRoomService.findChatRoomById(request.getRoomId());
            User sender = userService.findUserById(userId);

            chatRoomService.validateChatActivity(userId, request.getRoomId());

            boolean isFiltered = contentFilterService.isFiltered(request.getContent());

            if (isFiltered) {
                return handleFilteredMessage(chatRoom, sender, request);
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
    @Transactional
    public int markAsRead(Long userId, Long roomId) {
        ChatRoom chatRoom = chatRoomService.findChatRoomById(roomId);
        User user = userService.findUserById(userId);

        chatRoomService.validateChatRead(userId, roomId);

        Long lastMessageId = chatMessageService.findLatestMessageByChatRoomId(roomId)
                .map(ChatMessage::getId)
                .orElse(0L);

        chatRoom.markAsRead(user, lastMessageId);
        chatRoomService.save(chatRoom);

        chatPresenceService.resetUnreadCount(roomId, userId);
        String key = redisKeyManager.getReadStatusKey(roomId, userId);
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
        return 0;
    }

    @Override
    public void publishMessageEvent(ChatMessage savedMessage, Long recipientId, boolean recipientActive, String plainContent) {
        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(savedMessage.getId())
                .roomId(savedMessage.getChatRoom().getId())
                .senderId(savedMessage.getSender().getId())
                .content(savedMessage.getContent())
                .type(savedMessage.getType())
                .timestamp(savedMessage.getCreatedAt())
                .recipientId(recipientId)
                .recipientActive(recipientActive)
                .filtered(false)
                .encrypted(false)
                .plainContent(plainContent)
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent("chat-message-topic", event.getRoomId().toString(), event);
                }
            });
        } else {
            // 트랜잭션이 활성화되지 않은 경우(테스트 등) 직접 전송
            eventPublisher.publishEvent("chat-message-topic", event.getRoomId().toString(), event);
        }
    }

    private ChatMessageResponse handleFilteredMessage(ChatRoom chatRoom, User sender, ChatMessageRequest request) {
        String filteredContent = "[부적절한 내용이 감지되었습니다]";

        // 필터링된 메시지도 이벤트 발행 (분석용)
        ChatMessageEvent event = ChatMessageEvent.builder()
                .messageId(null) // 저장되지 않음
                .roomId(chatRoom.getId())
                .senderId(sender.getId())
                .content(request.getContent()) // 원본 내용 (분석용)
                .type(request.getType())
                .timestamp(LocalDateTime.now())
                .filtered(true)
                .build();

        eventPublisher.publishEvent("chat-message-topic", event.getRoomId().toString(), event);

        return ChatMessageResponse.filteredResponse(
                chatRoom.getId(),
                sender.getId(),
                chatRoom.getMatching().isAnonymous() ? "익명" : sender.getProfile().getNickname(),
                filteredContent,
                request.getType()
        );
    }

    private ChatMessageResponse handleNormalMessage(ChatRoom chatRoom, User sender, ChatMessageRequest request) {
        // 필터링 통과 시 저장 후 비동기 처리 시작
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType())
                .build();

        ChatMessage savedMessage = chatMessageService.save(chatMessage);
        chatRoom.updateLastMessageTime();

        User recipient = chatRoom.isListener(sender) ? chatRoom.getSpeaker() : chatRoom.getListener();
        boolean isRecipientActive = chatPresenceService.isUserActiveInRoom(recipient.getId(), chatRoom.getId());

        publishMessageEvent(savedMessage, recipient.getId(), isRecipientActive, request.getContent());

        return ChatMessageResponse.from(savedMessage, sender.getId());
    }
}
