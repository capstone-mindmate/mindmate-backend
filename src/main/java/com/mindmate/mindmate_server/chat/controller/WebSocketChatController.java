package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
/**
 * 클라이언트 요청 처리 담당
 */
public class WebSocketChatController {
    private final ChatService chatService;
    private final ChatPresenceService chatPresenceService;
    private final MessageReactionService messageReactionService;
    private final CustomFormService customFormService;

    private final ChatEventPublisher eventPublisher;

    /**
     * websocket을 통한 메시지 전송
     * 1. 메시지 필터링/DB 저장 (동기)
     * 2. Redis Pub/Sub으로 실시간 메시지 전달
     * 3. Kafka로 비동기 처리 이벤트 발행
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        eventPublisher.publishChatRoomEvent(request.getRoomId(), ChatEventType.MESSAGE, response);
    }


    /**
     * 사용자 상태 업데이트
     * Redis에 상태 저장 및 Pub/Sub 알림
     */
    @MessageMapping("/presence")
    public void updatePresence(
            @Payload PresenceRequest presenceRequest,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        boolean isOnline = "ONLINE".equals(presenceRequest.getStatus());

        chatPresenceService.updateUserStatus(
                userId,
                isOnline,
                presenceRequest.getActiveRoomId()
        );

        if (isOnline && presenceRequest.getActiveRoomId() != null) {
            chatService.markAsRead(userId, presenceRequest.getActiveRoomId());
        }
    }

    /**
     * 읽음 상태 업데이트 (WebSocket)
     * 1. DB 업데이트 및 Redis 캐시 갱신
     * 2. Redis Pub/Sub으로 실시간 읽음 상태 알림 -> 읽음 UI 처리?
     */
    @MessageMapping("/chat.read")
    public void markAsRead(
            @Payload ReadRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        chatService.markAsRead(userId, request.getRoomId());

        // 읽음 상태 알림 생성
        ReadStatusNotification notification = ReadStatusNotification.builder()
                .roomId(request.getRoomId())
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishChatRoomEvent(request.getRoomId(), ChatEventType.READ_STATUS, notification);
    }

    /**
     * 채팅에 감정표현 추가
     * Redis Pub/Sub을 통해 실시간 반응 전달
     */
    @MessageMapping("/chat.reaction")
    public void handleReaction(
            @Payload ReactionRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        MessageReactionResponse response = messageReactionService.addReaction(
                userId, request.getMessageId(), request.getReactionType()
        );
        eventPublisher.publishChatRoomEvent(request.getRoomId(), ChatEventType.REACTION, response);
    }

    /**
     * 커스텀폼 생성
     * Redis Pub/Sub으로 실시간 폼 전달
     */
    @MessageMapping("/chat.customform.create")
    public void createCustomForm(
            @Payload CustomFormRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        CustomFormResponse response = customFormService.createCustomForm(userId, request);

        eventPublisher.publishChatRoomEvent(request.getChatRoomId(), ChatEventType.CUSTOM_FORM, response);
    }

    /**
     * 커스텀폼 응답 제출
     * Redis Pub/Sub을 통한 실시간 응답 전달
     */
    @MessageMapping("/chat.customform.respond")
    public void respondToCustomForm(
            @Payload RespondToCustomFormRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        CustomFormResponse response = customFormService.respondToCustomForm(request.getFormId(), userId, request);
        eventPublisher.publishChatRoomEvent(request.getChatRoomId(), ChatEventType.CUSTOM_FORM_RESPONSE, response);
    }
}
