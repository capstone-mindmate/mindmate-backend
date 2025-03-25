package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import com.mindmate.mindmate_server.matching.dto.MatchingAcceptedEvent;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEventConsumer {
    private final WaitingUserRepository waitingUserRepository;
    private final RedisMatchingService redisMatchingService;

    @KafkaListener(topics = "matching-accepted", groupId = "matching-gruop")
    @Transactional
    public void handleMatchingAccepted(MatchingAcceptedEvent event){
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
            } catch (Exception e) {
                log.error("거절 실패: waitingUserId={}, error={}",
                        waitingUserId, e.getMessage());
            }
        });
    }
}
