package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User findUserById(Long userId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    List<User> findByCurrentRoleAndSuspensionEndTimeBefore(RoleType roleType, LocalDateTime time);

    List<Long> findAllUserIds();

    Optional<User> findByEmailOptional(String email);
}
