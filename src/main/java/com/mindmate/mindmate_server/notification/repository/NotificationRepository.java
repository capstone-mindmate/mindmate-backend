package com.mindmate.mindmate_server.notification.repository;

import com.mindmate.mindmate_server.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndReadNotificationIsFalseOrderByCreatedAtDesc(Long userId);

    void deleteByUserId(Long userId);

}