package com.mindmate.mindmate_server.matching.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.dto.ListenerStatus;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingEventConsumer {

    private final MatchingRepository matchingRepository;
    private final WaitingService waitingService;

    @KafkaListener(topics = "matching-events", groupId = "mindmate-matching")
    @Transactional
    public void consumeMatchingEvent(String message) {
        log.info("받은 메시지: {}", message);

        try {
            String[] parts = message.split(":");
            if (parts.length < 4) {
                log.error("메시지 형식이 유효하지 않습니다.: {}", message);
                return;
            }

            String eventType = parts[0];
            Long matchingId = Long.parseLong(parts[1]);
            Long speakerId = Long.parseLong(parts[2]);
            Long listenerId = Long.parseLong(parts[3]);

            Matching matching = matchingRepository.findById(matchingId)
                    .orElseThrow(() -> new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

            switch (eventType) {
                case "MATCHING_REQUESTED":
                    handleMatchingRequested(matching, speakerId, listenerId);
                    break;
                case "MATCHING_ACCEPTED":
                    handleMatchingAccepted(matching, speakerId, listenerId);
                    break;
                case "MATCHING_REJECTED":
                    handleMatchingRejected(matching, speakerId, listenerId);
                    break;
                case "MATCHING_CANCELED":
                    handleMatchingCanceled(matching, speakerId, listenerId);
                    break;
                case "MATCHING_COMPLETED":
                    handleMatchingCompleted(matching, speakerId, listenerId);
                    break;
                default:
                    log.warn("타입을 알 수 없습니다.: {}", eventType);
            }
        } catch (Exception e) {
            log.error("발생한 에러: {}", message, e);
        }
    }

    private void handleMatchingRequested(Matching matching, Long speakerId, Long listenerId) {
        log.info("요청 처리중... {}", matching.getId());

        // 매칭 요청 시 처리할 로직..
        // 채팅방 준비, 상태 업데이트 등
        // todo : 알림 발송

        if (matching.getInitiator().toString().equals("SPEAKER")) {
            // 스피커가 요청 -> 리스너에게 알림
            log.info("스피커 {} -> 리스너 {}", speakerId, listenerId);
        } else {
            // 리스너가 요청 -> 스피커에게 알림
            log.info("리스너 {} -> 스피커 {}", listenerId, speakerId);
        }
    }

    private void handleMatchingAccepted(Matching matching, Long speakerId, Long listenerId) {
        log.info("요청 수락중... {}", matching.getId());

        waitingService.updateListenerStatus(listenerId, ListenerStatus.BUSY);

        // todo : 채팅방 활성화 로직

        // todo : 수락 알림 추가
        if (matching.getInitiator().toString().equals("SPEAKER")) {
            // 스피커가 요청 -> 스피커
            log.info("스피커 {} -> 리스너 {}", speakerId, listenerId);
        } else {
            // 리스너가 요청 -> 리스너
            log.info("리스너 {} -> 스피커 {}", listenerId, speakerId);
        }
    }

    private void handleMatchingRejected(Matching matching, Long speakerId, Long listenerId) {
        log.info("요청 거절중... {}", matching.getId());

        String reason = matching.getRejectionReason();


        // todo : 알림
        if (matching.getInitiator().toString().equals("SPEAKER")) {
            log.info("스피커 {} -> 리스너 {} 거절 사유: {}",
                    speakerId, listenerId, reason);
        } else {
            log.info("리스너 {} -> 스피커 {} 거절 사유: {}",
                    listenerId, speakerId, reason);
        }
    }

    private void handleMatchingCanceled(Matching matching, Long speakerId, Long listenerId) {
        log.info("요청 취소중... {}", matching.getId());

        // 수락된 상태면 status 복원
        if (matching.getMatchedAt() != null) {
            waitingService.updateListenerStatus(listenerId, ListenerStatus.AVAILABLE);
        }

        // todo : 알림 추가
        if (matching.getInitiator().toString().equals("SPEAKER")) {
            log.info("스피커 {} -> 리스너 {}",
                    speakerId, listenerId);
        } else {
            log.info("리스너 {} -> 스피커 {}",
                    listenerId, speakerId);
        }
    }

    private void handleMatchingCompleted(Matching matching, Long speakerId, Long listenerId) {
        log.info("매칭 완료중 {}", matching.getId());

        waitingService.updateListenerStatus(listenerId, ListenerStatus.AVAILABLE);

        // todo : 상담 통계 업데이트
        log.info("리스너 {} && 스피커 {}",
                speakerId, listenerId);

        // todo : 후기 작성 알림은 나중에 구현
    }
}
