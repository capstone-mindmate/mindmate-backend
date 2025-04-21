package com.mindmate.mindmate_server.report.util;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SuspensionScheduler {
    private final UserService userService;

    // redis 이벤트 누락 시 처리
    @Scheduled(fixedRate = 21600000)
    public void checkAndUnsuspendUsers() {
        List<User> susendedUsers = userService.findByCurrentRoleAndSuspensionEndTimeBefore(
                RoleType.ROLE_SUSPENDED, LocalDateTime.now()
        );

        for (User user : susendedUsers) {
            user.unsuspend();
            userService.save(user);
        }
    }
}
