package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.service.ChatPresenceService;
import com.mindmate.mindmate_server.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {
    private final ChatService chatService;
    private final ChatPresenceService chatPresenceService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * websocket을 통한 메시지 전송
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat.room.{roomId}")
    public ChatMessageResponse sendMessage(
            @Payload ChatMessageRequest request,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        log.info("Received WebSocket message from user {}: {}", userId, request);
        return chatService.sendMessage(userId, request);
    }

    /**
     * 사용자 상태 업데이트
     */
    @MessageMapping("/presence")
    public void updatePresence(
            @Payload PresenceRequest presenceRequest,
            Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        log.info("Updating user presence: userId={}, status={}",
                userId, presenceRequest.getStatus());

        boolean isOnline = "ONLINE".equals(presenceRequest.getStatus());
        chatPresenceService.updateUserStatus(
                userId,
                isOnline,
                presenceRequest.getActiveRoomId()
        );
    }

    /**
     * 타이핑 상태 알림??
     * 사용자가 메시지 입력 중인거 알릴건가?
     */
//    @MessageMapping("/chat.typing")
//    public void notifyTyping(
//            @Payload TypingRequest request,
//            Principal principal) {
//        Long userId = Long.parseLong(principal.getName());

        // 타이핑 상태 정보 생성
//        TypingNotification notification = TypingNotification.builder()
//                .roomId(request.getRoomId())
//                .userId(userId)
//                .typing(request.isTyping())
//                .build();

        // 해당 채팅방의 상대방에게 타이핑 상태 전송
//        messagingTemplate.convertAndSend("/topic/chat.room." + request.getRoomId() + ".typing", notification);
//    }

    /**
     * 읽음 상태 업데이트 (WebSocket)
     * - 실시간으로 메시지 읽음 상태 업데이트
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

        // 해당 채팅방의 모든 참가자에게 읽음 상태 전송
        messagingTemplate.convertAndSend("/topic/chat.room." + request.getRoomId() + ".read", notification);
    }
}
