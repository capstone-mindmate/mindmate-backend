package com.mindmate.mindmate_server.notification.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.NotificationErrorCode;
import com.mindmate.mindmate_server.notification.domain.FCMToken;
import com.mindmate.mindmate_server.notification.dto.FCMTokenRequest;
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


}
