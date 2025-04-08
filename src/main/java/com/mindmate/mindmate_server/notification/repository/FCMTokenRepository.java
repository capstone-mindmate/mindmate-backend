package com.mindmate.mindmate_server.notification.repository;

import com.mindmate.mindmate_server.notification.domain.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    List<FCMToken> findByUserIdAndActiveIsTrue(Long userId);
    Optional<FCMToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM FCMToken t WHERE t.active = false AND t.modifiedAt < :date")
    int deleteByActiveIsFalseAndModifiedAtBefore(LocalDateTime date);
}
