package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.config.KafkaConfig;
import com.mindmate.mindmate_server.global.dto.DltMessageDTO;
import com.mindmate.mindmate_server.global.dto.DltTopicStatusDTO;
import com.mindmate.mindmate_server.global.util.KafkaConsumerFactory;
import com.mindmate.mindmate_server.global.util.KafkaTopicUtils;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DltMonitoringService {
    private final AdminClient adminClient;
    //    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaConsumerFactory consumerFactory;
    private final KafkaTopicUtils topicUtils;
    private final SlackNotifier slackNotifier;

    private static final int ALERT_THRESHOLD = 5;
    private static final Duration ALERT_INTERVAL = Duration.ofHours(1);

    private final Map<String, Instant> lastAlertTimeMap = new ConcurrentHashMap<>();

    /**
     * 모든 DLT 토픽 상태 조회
     */
    public List<DltTopicStatusDTO> getDltTopicStatus() {
        try {
            // 모든 토픽 조회
            List<String> allTopics = getAllTopics();

            // DLT 토픽만 필터링
            List<DltTopicStatusDTO> dltStatus = allTopics.stream()
                    .filter(topicUtils::isDltTopic)
                    .map(this::buildDltTopicStatus)
                    .collect(Collectors.toList());

            return dltStatus;
        } catch (Exception e) {
            log.error("DLT 토픽 상태 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("DLT 토픽 상태 조회 실패", e);
        }
    }

    /**
     * DLT 토픽 상태 정보 구성
     */
    private DltTopicStatusDTO buildDltTopicStatus(String dltTopic) {
        String originalTopic = topicUtils.extractOriginalTopic(dltTopic);
        String consumerGroup = topicUtils.extractConsumerGroup(dltTopic);
        long messageCount = getMessageCount(dltTopic);
        String lastMessageTime = getLastMessageTime(dltTopic);
        boolean isActive = messageCount > 0;
        String category = topicUtils.determineTopicCategory(dltTopic);

        List<String> consumerGroups = getConsumerGroupsForTopic(originalTopic);

        // 오류 유형별 count - 미구현
        Map<String, Long> errorCounts = null;

        return DltTopicStatusDTO.builder()
                .dltTopicName(dltTopic)
                .originalTopicName(originalTopic)
                .messageCount(messageCount)
                .lastMessageTime(lastMessageTime)
                .category(category)
                .active(isActive)
                .consumerGroups(consumerGroups)
                .errorCounts(errorCounts)
                .build();
    }

    /**
     * 특정 토픽의 메시지 수 조회
     */
    public long getMessageCount(String topic) {
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            Map<TopicPartition, Long> endOffsets = getTopicEndOffsets(consumer, topic);
            Map<TopicPartition, Long> beginningOffsets = getTopicBeginningOffsets(consumer, topic);

            return endOffsets.entrySet().stream()
                    .mapToLong(entry -> {
                        TopicPartition partition = entry.getKey();
                        long endOffset = entry.getValue();
                        long beginningOffset = beginningOffsets.getOrDefault(partition, 0L);
                        return Math.max(0, endOffset - beginningOffset);
                    })
                    .sum();
        } catch (Exception e) {
            log.error("토픽 {} 메시지 수 조회 실패: {}", topic, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 특정 토픽의 마지막 메시지 시간 조회
     */
    private String getLastMessageTime(String topic) {
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            Map<TopicPartition, Long> endOffsets = getTopicEndOffsets(consumer, topic);

            if (endOffsets.isEmpty() || endOffsets.values().stream().allMatch(offset -> offset == 0)) {
                return "N/A";
            }

            // 마지막 메시지 조회 로직
            for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
                TopicPartition partition = entry.getKey();
                long endOffset = entry.getValue();

                if (endOffset > 0) {
                    consumer.assign(Collections.singleton(partition));
                    consumer.seek(partition, endOffset - 1);

                    ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                    if (records != null && !records.isEmpty()) {
                        ConsumerRecord<String, Object> lastRecord = records.iterator().next();
                        return Instant.ofEpochMilli(lastRecord.timestamp()).toString();
                    }
                }
            }

            return "N/A";
        } catch (Exception e) {
            log.error("토픽 {} 마지막 메시지 시간 조회 실패: {}", topic, e.getMessage(), e);
            return "N/A";
        }
    }

    /**
     * 특정 DLT 토픽의 메시지 내용 조회
     */
    public List<DltMessageDTO> getDltMessages(String dltTopic, int limit) {
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            List<TopicPartition> partitions = consumer.partitionsFor(dltTopic).stream()
                    .map(info -> new TopicPartition(dltTopic, info.partition()))
                    .collect(Collectors.toList());

            if (partitions.isEmpty()) {
                return Collections.emptyList();
            }

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            List<DltMessageDTO> messages = new ArrayList<>();
            int fetchedCount = 0;

            while (fetchedCount < limit) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) {
                    break;
                }

                for (ConsumerRecord<String, Object> record : records) {
                    DltMessageDTO message = buildDltMessageDTO(record);
                    messages.add(message);
                    fetchedCount++;

                    if (fetchedCount >= limit) {
                        break;
                    }
                }
            }

            return messages;
        } catch (Exception e) {
            log.error("DLT 메시지 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("DLT 메시지 조회 실패", e);
        }
    }

    /**
     * 특정 컨슈머 그룹의 DLT 메시지 조회
     */
    public List<DltMessageDTO> getDltMessagesForGroup(String groupId, int limit) {
        List<String> dltTopics = findDltTopicsForGroup(groupId);

        if (dltTopics.isEmpty()) {
            return Collections.emptyList();
        }

        List<DltMessageDTO> allMessages = new ArrayList<>();
        int remainingLimit = limit;

        for (String dltTopic : dltTopics) {
            if (remainingLimit <= 0) {
                break;
            }

            List<DltMessageDTO> topicMessages = getDltMessages(dltTopic, remainingLimit);
            allMessages.addAll(topicMessages);
            remainingLimit -= topicMessages.size();
        }

        return allMessages;
    }

    /**
     * 특정 컨슈머 그룹과 관련된 DLT 토픽 찾기
     */
    public List<String> findDltTopicsForGroup(String groupId) {
        try {
            List<String> allTopics = getAllTopics();

            // 그룹 ID에서 접두사 추출
            String groupPrefix = groupId;
            if (groupId.endsWith("-group")) {
                groupPrefix = groupId.substring(0, groupId.length() - 6);
            }

            final String searchPattern = "-" + groupPrefix + "-group-dlt$";

            return allTopics.stream()
                    .filter(topic -> topic.matches(".*" + searchPattern))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("컨슈머 그룹 {} 관련 DLT 토픽 조회 실패: {}", groupId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 모든 카프카 토픽 목록 조회
     */
    public List<String> getAllTopics() {
        try {
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> topicNames = topicsResult.names().get();
            return new ArrayList<>(topicNames);
        } catch (Exception e) {
            log.error("토픽 목록 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("토픽 목록 조회 실패", e);
        }
    }

    /**
     * 특정 토픽과 관련된 컨슈머 그룹 조회
     */
    private List<String> getConsumerGroupsForTopic(String dltTopic) {
        try {
            String consumerGroup = topicUtils.extractConsumerGroup(dltTopic);
            if (consumerGroup == null) {
                return Collections.emptyList();
            }

            List<String> allGroups = getAllConsumerGroups();

            String groupPrefix = consumerGroup;
            if (consumerGroup.endsWith("-group")) {
                groupPrefix = consumerGroup.substring(0, consumerGroup.length() - 6);
            }

            final String prefix = groupPrefix;
            List<String> matchingGroups = allGroups.stream()
                    .filter(group -> group.startsWith(prefix))
                    .collect(Collectors.toList());

            if (!matchingGroups.isEmpty()) {
                return matchingGroups;
            }

            return Collections.singletonList(consumerGroup);
        } catch (Exception e) {
            log.error("DLT 토픽 {} 관련 컨슈머 그룹 조회 실패: {}", dltTopic, e.getMessage(), e);
            return Collections.emptyList();
        }
    }




    private List<String> getAllConsumerGroups() {
        try {
            ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groups = groupsResult.all().get();

            return groups.stream()
                    .map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("컨슈머 그룹 목록 조회 실패: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * DLT 토픽 모니터링 실행
     */
    @Scheduled(fixedDelayString = "${kafka.monitoring.interval:300000}")
    public void monitorDltTopics() {
        log.info("DLT 토픽 모니터링 실행 중...");

        try {
            List<DltTopicStatusDTO> dltStatus = getDltTopicStatus();

            for (DltTopicStatusDTO status : dltStatus) {
                if (status.getMessageCount() > ALERT_THRESHOLD) {
                    checkAndSendAlert(status);
                }
            }

            log.info("DLT 토픽 모니터링 완료: {} 토픽 확인됨", dltStatus.size());
        } catch (Exception e) {
            log.error("DLT 토픽 모니터링 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 임계값 초과 시 알림 전송
     */
    private void checkAndSendAlert(DltTopicStatusDTO status) {
        String dltTopic = status.getDltTopicName();
        Instant now = Instant.now();
        Instant lastAlertTime = lastAlertTimeMap.getOrDefault(dltTopic, Instant.EPOCH);

        if (Duration.between(lastAlertTime, now).compareTo(ALERT_INTERVAL) > 0) {
            // 샘플 오류 메시지 수집
            List<String> sampleErrors = collectSampleErrors(dltTopic, 3);

            // 알림 전송
            slackNotifier.sendKafkaDltAlert(
                    "임계값 초과",
                    dltTopic,
                    status.getMessageCount(),
                    status.getLastMessageTime(),
                    sampleErrors
            );

            // 마지막 알림 시간 업데이트
            lastAlertTimeMap.put(dltTopic, now);
        }
    }

    /**
     * DLT 토픽에서 샘플 오류 메시지 수집
     */
    private List<String> collectSampleErrors(String dltTopic, int limit) {
        try (Consumer<String, Object> consumer = consumerFactory.createConsumer()) {
            List<TopicPartition> partitions = consumer.partitionsFor(dltTopic).stream()
                    .map(info -> new TopicPartition(dltTopic, info.partition()))
                    .collect(Collectors.toList());

            if (partitions.isEmpty()) {
                return Collections.emptyList();
            }

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            List<String> errors = new ArrayList<>();
            int fetchedCount = 0;

            while (fetchedCount < limit) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) {
                    break;
                }

                for (ConsumerRecord<String, Object> record : records) {
                    // 헤더에서 오류 메시지 추출
                    Header errorHeader = record.headers().lastHeader(KafkaHeaders.EXCEPTION_MESSAGE);
                    if (errorHeader != null) {
                        String errorMessage = new String(errorHeader.value(), StandardCharsets.UTF_8);
                        errors.add(errorMessage);
                        fetchedCount++;

                        if (fetchedCount >= limit) {
                            break;
                        }
                    }
                }
            }

            return errors;
        } catch (Exception e) {
            log.error("샘플 오류 메시지 수집 실패: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * ConsumerRecord에서 DltMessageDTO 생성
     */
    private DltMessageDTO buildDltMessageDTO(ConsumerRecord<String, Object> record) {
        String originalTopic = null;
        String errorMessage = null;
        String consumerGroup = null;

        // 헤더에서 정보 추출
        for (Header header : record.headers()) {
            String key = header.key();
            byte[] value = header.value();

            if (key.equals(KafkaHeaders.ORIGINAL_TOPIC)) {
                originalTopic = new String(value, StandardCharsets.UTF_8);
            } else if (key.equals(KafkaHeaders.EXCEPTION_MESSAGE)) {
                errorMessage = new String(value, StandardCharsets.UTF_8);
            } else if (key.equals(KafkaHeaders.GROUP_ID)) {
                consumerGroup = new String(value, StandardCharsets.UTF_8);
            }
        }

        // 원본 토픽이 헤더에 없으면 토픽 이름에서 추출
        if (originalTopic == null) {
            originalTopic = topicUtils.extractOriginalTopic(record.topic());
        }

        // 컨슈머 그룹이 헤더에 없으면 토픽 이름에서 추출
        if (consumerGroup == null) {
            consumerGroup = topicUtils.extractConsumerGroup(record.topic());
        }

        return DltMessageDTO.builder()
                .key(record.key())
                .value(record.value())
                .timestamp(Instant.ofEpochMilli(record.timestamp()).toString())
                .partition(record.partition())
                .offset(record.offset())
                .originalTopic(originalTopic)
                .errorMessage(errorMessage)
                .consumerGroup(consumerGroup)
                .headers(convertHeadersToMap(record.headers()))
                .build();
    }

    /**
     * 헤더를 Map으로 변환
     */
    private Map<String, String> convertHeadersToMap(Headers headers) {
        Map<String, String> headerMap = new HashMap<>();
        headers.forEach(header ->
                headerMap.put(header.key(), new String(header.value(), StandardCharsets.UTF_8))
        );
        return headerMap;
    }

    /**
     * 토픽의 시작 오프셋 조회
     */
    private Map<TopicPartition, Long> getTopicBeginningOffsets(Consumer<String, Object> consumer, String topic) {
        List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(info -> new TopicPartition(topic, info.partition()))
                .collect(Collectors.toList());

        if (partitions.isEmpty()) {
            return Collections.emptyMap();
        }

        return consumer.beginningOffsets(partitions);
    }

    /**
     * 토픽의 끝 오프셋 조회
     */
    private Map<TopicPartition, Long> getTopicEndOffsets(Consumer<String, Object> consumer, String topic) {
        List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(info -> new TopicPartition(topic, info.partition()))
                .collect(Collectors.toList());

        if (partitions.isEmpty()) {
            return Collections.emptyMap();
        }

        return consumer.endOffsets(partitions);
    }
}
