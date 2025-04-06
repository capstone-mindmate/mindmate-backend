package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.NotificationErrorCode;
import com.mindmate.mindmate_server.notification.domain.FCMToken;
import com.mindmate.mindmate_server.notification.dto.FCMTokenResponse;
import com.mindmate.mindmate_server.notification.repository.FCMTokenRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

    private final FCMTokenRepository fcmTokenRepository;
    private final UserService userService;


    @Transactional
    public void registerToken(Long userId, String token) {
        User user = userService.findUserById(userId);

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
    }

    @Transactional
    public FCMTokenResponse deactivateToken(String token) {
        FCMToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(NotificationErrorCode.TOKEN_NOT_FOUND));
        // 실패 응답도 reponse에?

        fcmToken.deactivate();
        fcmTokenRepository.save(fcmToken);

        return FCMTokenResponse.builder()
                .success(true)
                .message("FCM 토큰이 비활성화되었습니다.")
                .build();
    }


}
