package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.DltMessageDTO;
import com.mindmate.mindmate_server.global.dto.DltTopicStatusDTO;
import com.mindmate.mindmate_server.global.util.KafkaConsumerFactory;
import com.mindmate.mindmate_server.global.util.KafkaTopicUtils;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DltMonitoringServiceTest {

    @Mock private AdminClient adminClient;
    @Mock private KafkaConsumerFactory consumerFactory;
    @Mock private KafkaTopicUtils topicUtils;
    @Mock private SlackNotifier slackNotifier;
    @Mock private Consumer<String, Object> mockConsumer;
    @Mock private ListTopicsResult listTopicsResult;

    @InjectMocks
    private DltMonitoringService dltMonitoringService;

    private static final String DLT_TOPIC = "user-events-user-group-dlt";
    private static final String ORIGINAL_TOPIC = "user-events";
    private static final String CONSUMER_GROUP = "user-group";

    @BeforeEach
    void setUp() {
        when(consumerFactory.createConsumer()).thenReturn(mockConsumer);
    }

    @Test
    @DisplayName("DLT 토픽 상태 조회 성공")
    void getDltTopicStatus_Success() {
        // given
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of(DLT_TOPIC)));
        when(topicUtils.isDltTopic(DLT_TOPIC)).thenReturn(true);
        when(topicUtils.extractOriginalTopic(DLT_TOPIC)).thenReturn(ORIGINAL_TOPIC);
        when(topicUtils.extractConsumerGroup(DLT_TOPIC)).thenReturn(CONSUMER_GROUP);
        when(topicUtils.determineTopicCategory(DLT_TOPIC)).thenReturn("USER");
        when(mockConsumer.partitionsFor(DLT_TOPIC)).thenReturn(List.of());

        // when
        List<DltTopicStatusDTO> result = dltMonitoringService.getDltTopicStatus();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDltTopicName()).isEqualTo(DLT_TOPIC);
        assertThat(result.get(0).getOriginalTopicName()).isEqualTo(ORIGINAL_TOPIC);
    }

    @Test
    @DisplayName("DLT 토픽 상태 조회 실패")
    void getDltTopicStatus_Failure() {
        // given
        when(adminClient.listTopics()).thenThrow(new RuntimeException("Connection failed"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dltMonitoringService.getDltTopicStatus());
        assertThat(exception.getMessage()).contains("DLT 토픽 상태 조회 실패");
    }

    @Test
    @DisplayName("메시지 수 조회 성공")
    void getMessageCount_Success() {
        // given
        TopicPartition partition = new TopicPartition(DLT_TOPIC, 0);
        when(mockConsumer.partitionsFor(DLT_TOPIC)).thenReturn(
                List.of(new PartitionInfo(DLT_TOPIC, 0, null, null, null))
        );
        when(mockConsumer.endOffsets(any())).thenReturn(Map.of(partition, 10L));
        when(mockConsumer.beginningOffsets(any())).thenReturn(Map.of(partition, 5L));

        // when
        long result = dltMonitoringService.getMessageCount(DLT_TOPIC);

        // then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("메시지 수 조회 실패 시 0 반환")
    void getMessageCount_ReturnsZero_WhenFails() {
        // given
        when(consumerFactory.createConsumer()).thenThrow(new RuntimeException("Consumer error"));

        // when
        long result = dltMonitoringService.getMessageCount(DLT_TOPIC);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("DLT 메시지 조회 성공")
    void getDltMessages_Success() {
        // given
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(DLT_TOPIC, 0, 0L, "key", "value");
        ConsumerRecords<String, Object> records = new ConsumerRecords<>(
                Map.of(new TopicPartition(DLT_TOPIC, 0), List.of(record))
        );

        when(mockConsumer.partitionsFor(DLT_TOPIC)).thenReturn(
                List.of(new PartitionInfo(DLT_TOPIC, 0, null, null, null))
        );
        when(mockConsumer.poll(Duration.ofMillis(500)))
                .thenReturn(records)
                .thenReturn(new ConsumerRecords<>(Map.of()));
        when(topicUtils.extractOriginalTopic(DLT_TOPIC)).thenReturn(ORIGINAL_TOPIC);
        when(topicUtils.extractConsumerGroup(DLT_TOPIC)).thenReturn(CONSUMER_GROUP);

        // when
        List<DltMessageDTO> result = dltMonitoringService.getDltMessages(DLT_TOPIC, 10);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalTopic()).isEqualTo(ORIGINAL_TOPIC);
    }

    @Test
    @DisplayName("DLT 메시지 조회 실패")
    void getDltMessages_Failure() {
        // given
        when(consumerFactory.createConsumer()).thenThrow(new RuntimeException("Consumer error"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dltMonitoringService.getDltMessages(DLT_TOPIC, 10));
        assertThat(exception.getMessage()).contains("DLT 메시지 조회 실패");
    }

    @Test
    @DisplayName("컨슈머 그룹별 DLT 토픽 찾기")
    void findDltTopicsForGroup_Success() {
        // given
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(
                KafkaFuture.completedFuture(Set.of(DLT_TOPIC, "other-topic"))
        );

        // when
        List<String> result = dltMonitoringService.findDltTopicsForGroup("user-group");

        // then
        assertThat(result).contains(DLT_TOPIC);
    }

    @Test
    @DisplayName("컨슈머 그룹별 DLT 메시지 조회")
    void getDltMessagesForGroup_Success() {
        // given
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(
                KafkaFuture.completedFuture(Set.of(DLT_TOPIC))
        );

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(DLT_TOPIC, 0, 0L, "key", "value");
        ConsumerRecords<String, Object> records = new ConsumerRecords<>(
                Map.of(new TopicPartition(DLT_TOPIC, 0), List.of(record))
        );

        when(mockConsumer.partitionsFor(DLT_TOPIC)).thenReturn(
                List.of(new PartitionInfo(DLT_TOPIC, 0, null, null, null))
        );
        when(mockConsumer.poll(Duration.ofMillis(500)))
                .thenReturn(records)
                .thenReturn(new ConsumerRecords<>(Map.of()));
        when(topicUtils.extractOriginalTopic(DLT_TOPIC)).thenReturn(ORIGINAL_TOPIC);
        when(topicUtils.extractConsumerGroup(DLT_TOPIC)).thenReturn(CONSUMER_GROUP);

        // when
        List<DltMessageDTO> result = dltMonitoringService.getDltMessagesForGroup(CONSUMER_GROUP, 5);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("모든 토픽 조회 성공")
    void getAllTopics_Success() {
        // given
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(
                KafkaFuture.completedFuture(Set.of(DLT_TOPIC, ORIGINAL_TOPIC))
        );

        // when
        List<String> result = dltMonitoringService.getAllTopics();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(DLT_TOPIC, ORIGINAL_TOPIC);
    }

    @Test
    @DisplayName("모든 토픽 조회 실패")
    void getAllTopics_Failure() {
        // given
        when(adminClient.listTopics()).thenThrow(new RuntimeException("Admin error"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dltMonitoringService.getAllTopics());
        assertThat(exception.getMessage()).contains("토픽 목록 조회 실패");
    }

    @Test
    @DisplayName("DLT 토픽 모니터링 실행 성공")
    void monitorDltTopics_Success() {
        // given
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of()));

        // when & then
        assertDoesNotThrow(() -> dltMonitoringService.monitorDltTopics());
    }

    @Test
    @DisplayName("임계값 초과 시 알림 전송")
    void monitorDltTopics_SendsAlert_WhenThresholdExceeded() {
        // given
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of(DLT_TOPIC)));
        when(topicUtils.isDltTopic(DLT_TOPIC)).thenReturn(true);
        when(topicUtils.extractOriginalTopic(DLT_TOPIC)).thenReturn(ORIGINAL_TOPIC);
        when(topicUtils.extractConsumerGroup(DLT_TOPIC)).thenReturn(CONSUMER_GROUP);
        when(topicUtils.determineTopicCategory(DLT_TOPIC)).thenReturn("USER");

        TopicPartition partition = new TopicPartition(DLT_TOPIC, 0);
        when(mockConsumer.partitionsFor(DLT_TOPIC)).thenReturn(
                List.of(new PartitionInfo(DLT_TOPIC, 0, null, null, null))
        );
        when(mockConsumer.endOffsets(any())).thenReturn(Map.of(partition, 15L));
        when(mockConsumer.beginningOffsets(any())).thenReturn(Map.of(partition, 5L));
        when(mockConsumer.poll(Duration.ofMillis(500))).thenReturn(new ConsumerRecords<>(Map.of()));

        // when
        dltMonitoringService.monitorDltTopics();

        // then
        verify(slackNotifier).sendKafkaDltAlert(
                eq("임계값 초과"),
                eq(DLT_TOPIC),
                eq(10L),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("모니터링 실행 중 예외 발생 시 로그만 기록")
    void monitorDltTopics_LogsException_WhenErrorOccurs() {
        // given
        when(adminClient.listTopics()).thenThrow(new RuntimeException("Monitoring error"));

        // when & then
        assertDoesNotThrow(() -> dltMonitoringService.monitorDltTopics());
    }
}
