package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.DlqMessageDTO;
import com.mindmate.mindmate_server.global.dto.DlqTopicStatusDTO;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqMonitoringService {
    private final KafkaAdminClient kafkaAdminClient;
    private final SlackNotifier slackNotifier;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Scheduled(fixedRate = 3000000) // 5분
    public void monitorDlqTopics() {
        try {
            // 모든 DLQ 토픽 패턴 조회
            Pattern dlqPattern = Pattern.compile(".*-dlq");
            Set<String> dlqTopics = kafkaAdminClient.listTopics().names().get().stream()
                    .filter(topic -> dlqPattern.matcher(topic).matches())
                    .collect(Collectors.toSet());

            // 각 DLQ 토픽의 메시지 수 확인
            for (String dlqTopic : dlqTopics) {
                Map<TopicPartition, Long> endOffsets = getOffsets(dlqTopic, true);
                Map<TopicPartition, Long> beginOffsets = getOffsets(dlqTopic, false);

                long totalMessages = calculateTotalMessages(beginOffsets, endOffsets);
                if (totalMessages > 0) {
                    log.warn("DLQ 토픽 {} 에 {} 개의 메시지가 있습니다", dlqTopic, totalMessages);

                    // 임계값 설정하고 너무 많아지면 slack에 noti
                    if (totalMessages > 1) {
                        slackNotifier.sendKafkaDLQAlert("DLQ_THRESHOLD_EXCEEDED",
                                String.format("DLQ 토픽 %s에 %d개의 메시지가 쌓여있습니다. 확인이 필요합니다.", dlqTopic, totalMessages));
                    }
                }
            }
        } catch (Exception e) {
            log.error("DLQ 모니터링 중 오류 발생", e);
        }
    }

    public List<DlqTopicStatusDTO> getDlqTopicStatus() {
        List<DlqTopicStatusDTO> result = new ArrayList<>();

        try {
            Pattern dlqPattern = Pattern.compile(".*-dlq");
            Set<String> dlqTopics = kafkaAdminClient.listTopics().names().get().stream()
                    .filter(topic -> dlqPattern.matcher(topic).matches())
                    .collect(Collectors.toSet());

            for (String dlqTopic : dlqTopics) {
                String originalTopic = dlqTopic.replace("-dlq", "");

                Map<TopicPartition, Long> endOffsets = getOffsets(dlqTopic, true);
                Map<TopicPartition, Long> beginOffsets = getOffsets(dlqTopic, false);

                long messageCount = calculateTotalMessages(beginOffsets, endOffsets);

                String lastMessageTime = "N/A";
                if (messageCount > 0) {
                    try {
                        lastMessageTime = getLastMessageTimestamp(dlqTopic);
                    } catch (Exception e) {
                        log.warn("마지막 메시지 시간 조회 실패: {}", e.getMessage());
                    }
                }

                String category = extractCategory(originalTopic);

                DlqTopicStatusDTO statusDTO = DlqTopicStatusDTO.builder()
                        .dlqTopicName(dlqTopic)
                        .originalTopicName(originalTopic)
                        .messageCount(messageCount)
                        .lastMessageTime(lastMessageTime)
                        .category(category)
                        .active(messageCount > 0)
                        .build();

                result.add(statusDTO);
            }
            result.sort(Comparator.comparing(DlqTopicStatusDTO::getMessageCount).reversed());
        } catch (Exception e) {
            log.error("DLQ 토픽 상태 조회 중 에러 발생", e);
        }
        return result;
    }

    private String extractCategory(String topicName) {
        if (topicName.contains("chat")) {
            return "Chat";
        } else if (topicName.contains("magazine")) {
            return "Magazine";
        } else if (topicName.contains("matching")) {
            return "Matching";
        } else {
            return "Other";
        }
    }

    private String getLastMessageTimestamp(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "timestamp-checker-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return "N/A";
            }

            List<TopicPartition> topicPartitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .collect(Collectors.toList());

            consumer.assign(topicPartitions);

            // 각 파티션의 마지막 오프셋 - 1 위치로 이동
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);
            for (TopicPartition partition : topicPartitions) {
                long lastOffset = endOffsets.get(partition);
                if (lastOffset > 0) {
                    consumer.seek(partition, lastOffset - 1);
                }
            }

            // 마지막 메시지 조회
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            if (!records.isEmpty()) {
                // 가장 최근 타임스탬프 찾기
                long latestTimestamp = records.iterator().next().timestamp();
                for (ConsumerRecord<String, String> record : records) {
                    if (record.timestamp() > latestTimestamp) {
                        latestTimestamp = record.timestamp();
                    }
                }
                return Instant.ofEpochMilli(latestTimestamp).toString();
            }

            return "N/A";
        } catch (Exception e) {
            log.warn("마지막 메시지 타임스탬프 조회 실패: {}", e.getMessage());
            return "Error";
        }
    }

    /**
     * 특정 DLQ 토픽의 메시지 내용 조회
     */
    public List<DlqMessageDTO> getDlqMessages(String topic, int limit) {
        List<DlqMessageDTO> messages = new ArrayList<>();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-viewer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, limit);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props)) {
            // DLQ 토픽 구독
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return messages;
            }

            List<TopicPartition> totalPartitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .collect(Collectors.toList());

            consumer.assign(totalPartitions);
            consumer.seekToBeginning(totalPartitions);

            int messageCount = 0;
            while (messageCount < limit) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    break;
                }

                for (ConsumerRecord<String, Object> record : records) {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("key", record.key());
                    messageMap.put("value", record.value());
                    messageMap.put("timestamp", Instant.ofEpochMilli(record.timestamp()).toString());
                    messageMap.put("partition", record.partition());
                    messageMap.put("offset", record.offset());

                    messages.add(DlqMessageDTO.from(messageMap, topic));
                    messageCount++;

                    if (messageCount >= limit) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("DLQ 메시지 조회 중 오류 발생: {}", e.getMessage(), e);
        }
        return messages;
    }

    private long calculateTotalMessages(Map<TopicPartition, Long> beginOffsets, Map<TopicPartition, Long> endOffsets) {
        long total = 0;
        for (TopicPartition partition : endOffsets.keySet()) {
            long end = endOffsets.get(partition);
            long begin = beginOffsets.getOrDefault(partition, 0L);
            total += (end - begin);
        }
        return total;
    }

    private Map<TopicPartition, Long> getOffsets(String topic, boolean isEnd) throws Exception {
        // todo: pros를 여기서 이렇게 설정하는게 맞나?
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "offset-checker-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, isEnd ? "latest" : "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                return Collections.emptyMap();
            }

            List<TopicPartition> topicPartitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .collect(Collectors.toList());

            consumer.assign(topicPartitions);
            return isEnd ? consumer.endOffsets(topicPartitions) : consumer.beginningOffsets(topicPartitions);
        }
    }

    public List<String> getAllTopics() {
        try {
            return new ArrayList<>(kafkaAdminClient.listTopics().names().get());
        } catch (Exception e) {
            log.error("토픽 목록 조회 실패: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
