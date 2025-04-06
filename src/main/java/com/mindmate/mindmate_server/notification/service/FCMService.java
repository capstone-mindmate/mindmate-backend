package com.mindmate.mindmate_server.notification.service;

import com.google.firebase.messaging.*;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.NotificationErrorCode;
import com.mindmate.mindmate_server.notification.domain.FCMToken;
import com.mindmate.mindmate_server.notification.dto.FCMTokenRequest;
import com.mindmate.mindmate_server.notification.dto.FCMTokenResponse;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.repository.FCMTokenRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

    private final FCMTokenRepository fcmTokenRepository;
    private final UserService userService;
    private final FirebaseMessaging firebaseMessaging;

    @Transactional
    public FCMTokenResponse registerToken(Long userId, FCMTokenRequest request) {
        User user = userService.findUserById(userId);
        String token = request.getToken();

        fcmTokenRepository.findByToken(token).ifPresentOrElse(
            existingToken -> {
                if (!existingToken.getUser().getId().equals(userId)) {
                    existingToken.deactivate();

                    FCMToken newToken = FCMToken.builder()
                        .user(user)
                        .token(token)
                        .build();
                    fcmTokenRepository.save(newToken);
                }
            },
            () -> {
                FCMToken newToken = FCMToken.builder()
                    .user(user)
                    .token(token)
                    .build();
                fcmTokenRepository.save(newToken);
            }
        );

        return FCMTokenResponse.builder()
                .success(true)
                .message("FCM 토큰이 성공적으로 등록되었습니다.")
                .build();
    }

    @Transactional
    public FCMTokenResponse deactivateToken(Long userId, FCMTokenRequest request) {
        FCMToken fcmToken = fcmTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new CustomException(NotificationErrorCode.TOKEN_NOT_FOUND));
        // 실패 응답도 reponse에?

        if (!fcmToken.getUser().getId().equals(userId)) {
            throw new CustomException(NotificationErrorCode.TOKEN_UNAUTHORIZED);
        }

        fcmToken.deactivate();
        fcmTokenRepository.save(fcmToken);

        return FCMTokenResponse.builder()
            .success(true)
            .message("FCM 토큰이 비활성화되었습니다.")
            .build();
    }


    public void sendNotification(Long userId, NotificationEvent event) {
        List<FCMToken> tokens = fcmTokenRepository.findByUserIdAndActiveIsTrue(userId);

        if (tokens.isEmpty()) {
            log.info("사용자 ID {}에 대한 활성 FCM 토큰이 없습니다.", userId);
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(event.getTitle())
                .setBody(event.getContent())
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", UUID.randomUUID().toString());
        data.put("type", event.getType());
        data.put("relatedEntityId", event.getRelatedEntityId().toString());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        for (FCMToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(notification)
                        .putAllData(data)
                        .build();

                String response = firebaseMessaging.send(message);
                log.debug("FCM 알림 전송 성공 - 토큰: {}, 응답: {}", token.getToken(), response);

            } catch (FirebaseMessagingException e) {
                handleFCMSendError(token, e);
            }
        }
    }


    private void handleFCMSendError(FCMToken token, FirebaseMessagingException e) {
        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            log.warn("유효하지 않은 FCM 토큰 비활성화: {}", token.getToken());
            token.deactivate();
            fcmTokenRepository.save(token);
        } else {
            log.error("FCM 전송 오류: {}, 토큰: {}", e.getMessage(), token.getToken());
        }
    }
}
