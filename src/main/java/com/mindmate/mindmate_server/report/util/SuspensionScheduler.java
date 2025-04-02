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

    // todo: unsuspend하는 방식 다른 방식 고려 -> AOP 이용해서 api 호출 시 확인? 너무 비효율적 아닌가
    @Scheduled(fixedRate = 3600000)
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
