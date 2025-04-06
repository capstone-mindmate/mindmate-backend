package com.mindmate.mindmate_server.notification.repository;

import com.mindmate.mindmate_server.notification.domain.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    Optional<FCMToken> findByToken(String token);
}
