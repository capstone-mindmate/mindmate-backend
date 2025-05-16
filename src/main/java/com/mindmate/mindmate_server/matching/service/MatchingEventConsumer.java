package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEventConsumer {
    private final WaitingUserRepository waitingUserRepository;
    private final RedisMatchingService redisMatchingService;

//    @KafkaStandardRetry
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000),
            dltTopicSuffix = "-matching-group-dlt",
            retryTopicSuffix = "-matching-group-retry"
    )
    @KafkaListener(topics = "matching-accepted", groupId = "matching-group")
    @Transactional
    public void handleMatchingAccepted(MatchingAcceptedEvent event, Acknowledgment ack){
        event.getPendingWaitingUserIds().forEach(waitingUserId -> {
            try {
                WaitingUser waitingUser = waitingUserRepository.findById(waitingUserId)
                        .orElse(null);

                if (waitingUser != null) {
                    waitingUser.reject();
                    waitingUserRepository.save(waitingUser);

                    redisMatchingService.decrementUserActiveMatchingCount(
                            waitingUser.getWaitingUser().getId());

                    log.info("거절 완료: waitingUserId={}", waitingUserId);
                }
                ack.acknowledge();
            } catch (Exception e) {
                log.error("거절 실패: waitingUserId={}", waitingUserId, e);
                throw e;
            }
        });
    }
}
