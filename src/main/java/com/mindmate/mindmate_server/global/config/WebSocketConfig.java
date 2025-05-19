package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.auth.util.JwtTokenProvider;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtTokenProvider jwtTokenProvider;
    /**
     * WebSocket 연결 관리 정리
     * 1. 연결 시점: 클라이언트가 /ws 엔드포인트로 연결 요청 시
     * 2. 인증: 'Authorization' 헤더의 JWT 토큰으로 인증
     * 3. 연결 이벤트 처리 -> WebSocketEventListener에서 처리 -> 사용자 상태 업데이트
     * 4. 하트비트: 10초 간격으로 서버-클라이언트 간 연결 상태 확인
     */

    /**
     * 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // topic -> 일대다 메시징을 위한 topic 기반 메시지 브로딩캐스팅
        // queue -> 일대일 메시징을 위한 큐 기반 메시지 전달
        // heartbeat -> 클라이언트-서버 간 연결 상태 확인 (30초)
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(heartBeatScheduler())
                .setHeartbeatValue(new long[] {30000, 30000});

        config.setApplicationDestinationPrefixes("/app"); // 클라이언트 메시지 전송 경로
        config.setUserDestinationPrefix("/user"); // 특정 사용자에게 메시지 보낼 때 사용할 prefix
    }

    /**
     * 웹소켓 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setDisconnectDelay(30 * 1000) // 연결 해제 지연 시간 30초
                .setHeartbeatTime(25 * 1000); // SockJS 하트비트 25초
    }

    /**
     * Websocket 전송 관련 설정
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setSendTimeLimit(30 * 1000) // 메시지 전송 제한 시간 30초
                .setSendBufferSizeLimit(512 * 1024) // 버퍼 크기 512KB
                .setMessageSizeLimit(128 * 1024) // 메시지 크기 128KB
                .setTimeToFirstMessage(60 * 1000); // 첫 메시지 대기 시간 60초
    }

    /**
     * heartbeat 처리를 위한 스케줄러 설정
     */
    @Bean
    public ThreadPoolTaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3); // 단일 스레드
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }

    /**
     * 웹소켓 연결 시 "Authorization" 헤더의 JWT 토큰 기반 인증 처리
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    log.info("WebSocket Connection attempt with token: {}", token);

                    if (token != null && token.startsWith("Bearer ")) {
                        try {
                            String jwtToken = token.substring(7);
                            Long userId = jwtTokenProvider.getUserIdFromToken(jwtToken);

                            accessor.setUser(new UserPrincipal(userId));
                            log.info("WebSocket Authentication successful for user: {}", userId);
                        } catch (Exception e) {
                            log.error("WebSocket Authentication failed: {}", e.getMessage());
                            throw new MessageDeliveryException("Authentication failed");
                        }
                    } else {
                        log.error("WebSocket Connection without valid token");
                        throw new MessageDeliveryException("No valid token provided");
                    }
                }
                return message;
            }
        });
    }

}
