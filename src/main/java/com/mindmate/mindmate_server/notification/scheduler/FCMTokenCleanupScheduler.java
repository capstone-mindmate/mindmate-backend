package com.mindmate.mindmate_server.notification.scheduler;

import com.mindmate.mindmate_server.notification.repository.FCMTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class FCMTokenCleanupScheduler {

    private final FCMTokenRepository fcmTokenRepository;

 // 새벽 세 시에 실행
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldTokens() {
        log.info("FCM 토큰 정리 작업 시작");

        try {
            // 3개월 이상 비활성화된 것들
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);

            int deletedCount = fcmTokenRepository.deleteByActiveIsFalseAndModifiedAtBefore(cutoffDate);

            log.info("비활성화된 FCM 토큰 정리 완료: {} 개 삭제됨", deletedCount);
        } catch (Exception e) {
            log.error("FCM 토큰 정리 중 오류 발생", e);
        }
    }

}