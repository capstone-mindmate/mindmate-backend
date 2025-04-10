package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {
    User findUserById(Long userId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

//    boolean existsByNickname(String nickname);

    void save(User user);

    User findVerificationToken(String token);

    List<User> findByCurrentRoleAndSuspensionEndTimeBefore(RoleType roleType, LocalDateTime time);

    List<Long> findAllUserIds();
}
