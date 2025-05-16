package com.mindmate.mindmate_server.global.util;

import com.mindmate.mindmate_server.global.service.DltReprocessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalDltHandler {
    private final KafkaTopicUtils topicUtils;
    private final SlackNotifier slackNotifier;
//    private final DltMonitoringService dltMonitoringService;
    private final DltReprocessingService dltReprocessingService;

    // 알림 제한을 위한 상태 관리
    private final Map<String, Instant> recentAlerts = new ConcurrentHashMap<>();
    private final Map<String, List<String>> aggregatedErrors = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();

    private static final Duration ALERT_INTERVAL = Duration.ofMinutes(5);
    private static final int MAX_ERRORS_TO_REPORT = 1;
    private static final int AUTO_REPROCESS_THRESHOLD = 1;

    /**
     * DLT 메시지 처리 메서드
     * RetryTopicConfiguration에서 dltHandlerMethod로 지정됨
     */
    public void handleDltMessage(ConsumerRecord<?, ?> record,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partitionId,
                                 @Header(KafkaHeaders.OFFSET) Long offset,
                                 @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage,
                                 @Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace,
                                 @Header(value = KafkaHeaders.GROUP_ID, required = false) String groupId) {

        String originalTopic = topicUtils.extractOriginalTopic(topic);
        String consumerGroup = groupId != null ? groupId : topicUtils.extractConsumerGroup(topic);

        log.warn("DLT 메시지 수신: topic={}, partition={}, offset={}, originalTopic={}, consumerGroup={}",
                topic, partitionId, offset, originalTopic, consumerGroup);

        if (errorMessage != null) {
            log.error("DLT 오류 메시지: {}", errorMessage);
            if (stackTrace != null) {
                log.debug("스택 트레이스: {}", stackTrace);
            }
        }

        String failureKey = originalTopic + "-" + consumerGroup;
        int currentFailures = failureCounters.computeIfAbsent(failureKey, k -> new AtomicInteger(0))
                .incrementAndGet();

        log.info("토픽 {} (컨슈머 그룹: {}) 누적 실패 횟수: {}",
                originalTopic, consumerGroup, currentFailures);

        processErrorForAggregation(failureKey, errorMessage, record.key() != null ? record.key().toString() : "null");

        sendAlertIfNeeded(topic, originalTopic, consumerGroup, errorMessage, currentFailures);

        // 자동 재처리 로직 (특정 조건에 따라)
//        considerAutoReprocessing(topic, originalTopic, consumerGroup, currentFailures, errorMessage);
    }

    /**
     * 집계된 오류 알림 전송 (스케줄링)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void sendAggregatedAlerts() {
        if (aggregatedErrors.isEmpty()) {
            return;
        }

        Map<String, List<String>> currentErrors = new HashMap<>(aggregatedErrors);
        aggregatedErrors.clear();

        // 토픽별로 집계된 알림 전송
        currentErrors.forEach((key, errors) -> {
            String[] parts = key.split("-", 2);
            if (parts.length == 2) {
                String topic = parts[0];
                String group = parts[1];

                // DLT 토픽 이름 구성
                String dltTopic = topicUtils.buildDltTopicName(topic, group);

                slackNotifier.sendKafkaDltAlert(
                        "AGGREGATED_FAILURES",
                        dltTopic,
                        errors.size(),
                        Instant.now().toString(),
                        errors.stream().limit(MAX_ERRORS_TO_REPORT).collect(Collectors.toList())
                );
            }
        });
    }

    /**
     * 오류 메시지 집계 처리
     */
    private void processErrorForAggregation(String failureKey, String errorMessage, String messageKey) {
        String errorDetail = String.format(
                "메시지 키: %s, 오류: %s",
                messageKey,
                errorMessage != null ? errorMessage : "알 수 없는 오류"
        );

        aggregatedErrors.computeIfAbsent(failureKey, k -> new ArrayList<>()).add(errorDetail);

        // 집계 목록이 너무 커지지 않도록 제한
        List<String> errors = aggregatedErrors.get(failureKey);
        if (errors.size() > 100) {
            errors = errors.subList(0, 100);
            aggregatedErrors.put(failureKey, errors);
        }
    }

    /**
     * 알림 전송 여부 결정 및 전송
     */
    private void sendAlertIfNeeded(String dltTopic, String originalTopic, String consumerGroup,
                                   String errorMessage, int currentFailures) {
        String alertKey = originalTopic + "-" + consumerGroup + "-alert";
        Instant lastAlert = recentAlerts.get(alertKey);
        Instant now = Instant.now();

        // 첫 번째 실패 또는 일정 시간 경과 후 알림 전송
        if (currentFailures == 1 ||
                (lastAlert == null || lastAlert.plus(ALERT_INTERVAL).isBefore(now))) {

            recentAlerts.put(alertKey, now);

            String alertType = currentFailures == 1 ? "FIRST_FAILURE" : "RECURRING_FAILURES";
            List<String> errorSamples = Collections.singletonList(
                    String.format("컨슈머 그룹 %s에서 메시지 처리 실패: %s",
                            consumerGroup,
                            errorMessage != null ? errorMessage : "알 수 없는 오류")
            );

            slackNotifier.sendKafkaDltAlert(
                    alertType,
                    dltTopic,
                    currentFailures,
                    now.toString(),
                    errorSamples
            );
        }
    }

    /**
     * 자동 재처리 고려
     */
    private void considerAutoReprocessing(String dltTopic, String originalTopic, String consumerGroup,
                                          int currentFailures, String errorMessage) {
        // 특정 유형의 오류는 자동 재처리 제외
        if (errorMessage != null &&
                (errorMessage.contains("DeserializationException") ||
                        errorMessage.contains("SerializationException") ||
                        errorMessage.contains("AuthorizationException"))) {
            log.warn("자동 재처리에 부적합한 오류 유형: {}", errorMessage);
            return;
        }

        if (currentFailures == AUTO_REPROCESS_THRESHOLD) {
            log.info("임계값({})에 도달하여 자동 재처리 시도: {}",
                    AUTO_REPROCESS_THRESHOLD, dltTopic);

            try {
                int processedCount = dltReprocessingService.reprocessDltMessage(dltTopic, 10);

                if (processedCount > 0) {
                    log.info("자동 재처리 완료: {} 메시지 처리됨", processedCount);

                    // 재처리 후 카운터 리셋
                    failureCounters.put(originalTopic + "-" + consumerGroup, new AtomicInteger(0));

                    slackNotifier.sendKafkaDltAlert(
                            "AUTO_REPROCESSED",
                            dltTopic,
                            processedCount,
                            Instant.now().toString(),
                            Collections.singletonList("시스템에 의해 자동 재처리되었습니다.")
                    );
                }
            } catch (Exception e) {
                log.error("자동 재처리 실패: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 실패 카운터 리셋 (재처리 성공 후 호출)
     */
    public void resetFailureCounter(String originalTopic, String consumerGroup) {
        String failureKey = originalTopic + "-" + consumerGroup;
        failureCounters.put(failureKey, new AtomicInteger(0));
        log.info("실패 카운터 리셋: topic={}, group={}", originalTopic, consumerGroup);
    }
}