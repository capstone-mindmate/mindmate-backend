package com.mindmate.mindmate_server.chat.util;

import com.mindmate.mindmate_server.chat.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final ChatPresenceService chatPresenceService;

    /**
     * 사용자가 웹소켓에 연결될 때 사용자의 상태 온라인으로 업데이트
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        Principal user = headerAccessor.getUser();
        if (user != null) {
            Long userId = Long.parseLong(user.getName());
            log.info("User connected: {}", userId);
            chatPresenceService.updateUserStatus(userId, true, null);
        }
    }

    /**
     * 웹소켓 연결을 끊을 때 사용자 상태를 오프라인으로 업데이트
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        Principal user = headerAccessor.getUser();
        if (user != null) {
            Long userId = Long.parseLong(user.getName());
            log.info("User disconnected: {}", userId);
            chatPresenceService.updateUserStatus(userId, false, null);
        }
    }
}

