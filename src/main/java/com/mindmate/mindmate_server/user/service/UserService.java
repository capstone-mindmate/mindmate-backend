package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.user.domain.User;

public interface UserService {
    User findUserById(Long userId);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    void save(User user);

    User findVerificationToken(String token);
}
