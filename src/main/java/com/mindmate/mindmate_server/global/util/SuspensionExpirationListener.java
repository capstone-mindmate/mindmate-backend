package com.mindmate.mindmate_server.global.util;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuspensionExpirationListener implements MessageListener {
    private final UserService userService;

    /**
     * 키 만료 이벤트 수신하여 해당 사용자의 정지 해제
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());

        if (expiredKey.startsWith("user:suspension:")) {
            try {
                String userId = expiredKey.substring("user:suspension:".length());
                Long userIdLong = Long.parseLong(userId);

                User user = userService.findUserById(userIdLong);
                if (user.getCurrentRole() == RoleType.ROLE_SUSPENDED) {
                    user.unsuspend();
                    userService.save(user);
                    log.info("User {} unsuspended successfully", userIdLong);
                }
            } catch (Exception e) {
                log.error("Error processing suspension expiration: {}", e.getMessage(), e);
            }
        }
    }
}
