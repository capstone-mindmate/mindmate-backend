package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.*;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaHealthServiceTest {

    @Mock private KafkaAdminClient kafkaAdminClient;
    @Mock private SlackNotifier slackNotifier;
    @Mock private DescribeClusterResult clusterResult;
    @Mock private ListTopicsResult topicsResult;
    @Mock private ListConsumerGroupsResult consumerGroupsResult;

    @InjectMocks
    private KafkaHealthService kafkaHealthService;

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int BROKER_COUNT = 3;
    private static final int TOPIC_COUNT = 10;
    private static final int DLT_TOPIC_COUNT = 2;

    private Collection<Node> brokerNodes;
    private Set<String> topicNames;
    private Collection<ConsumerGroupListing> consumerGroups;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaHealthService, "bootstrapServers", BOOTSTRAP_SERVERS);

        brokerNodes = createMockBrokerNodes();
        topicNames = createMockTopicNames();
        consumerGroups = createMockConsumerGroups();
    }

    @Test
    @DisplayName("카프카 상태 확인 성공")
    void checkKafkaHealth_ShouldReturnHealthyStatus() {
        // given
        setupHealthyKafkaMocks();

        // when
        KafkaHealthDTO result = kafkaHealthService.checkKafkaHealth();

        // then
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getBrokerCount()).isEqualTo(BROKER_COUNT);
        assertThat(result.getTopicCount()).isEqualTo(TOPIC_COUNT);
        assertThat(result.getDltTopicCount()).isEqualTo(DLT_TOPIC_COUNT);
        assertThat(result.getBrokers()).hasSize(BROKER_COUNT);
        assertThat(result.getConsumerGroups()).hasSize(2);

        verify(kafkaAdminClient, times(2)).describeCluster();
        verify(kafkaAdminClient, times(2)).listTopics();
    }


    @Test
    @DisplayName("카프카 연결 실패 시 비정상 상태 반환")
    void checkKafkaHealth_ShouldReturnUnhealthyStatus_WhenConnectionFails() {
        // given
        when(kafkaAdminClient.describeCluster()).thenThrow(new RuntimeException("Connection failed"));

        // when
        KafkaHealthDTO result = kafkaHealthService.checkKafkaHealth();

        // then
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getErrorMessage()).contains("Connection failed");
    }

    @Test
    @DisplayName("브로커 정보 조회 성공")
    void getBrokerInfo_ShouldReturnBrokerList() {
        // given
        setupBrokerInfoMocks();

        // when
        List<BrokerInfoDTO> result = kafkaHealthService.getBrokerInfo();

        // then
        assertThat(result).hasSize(BROKER_COUNT);
        BrokerInfoDTO firstBroker = result.get(0);
        assertThat(firstBroker.getId()).isEqualTo(1);
        assertThat(firstBroker.getHost()).isEqualTo("broker1");
        assertThat(firstBroker.getPort()).isEqualTo(9092);
    }

    @Test
    @DisplayName("브로커 정보 조회 실패 시 빈 리스트 반환")
    void getBrokerInfo_ShouldReturnEmptyList_WhenClusterDescribeFails() {
        // given
        when(kafkaAdminClient.describeCluster()).thenThrow(new RuntimeException("Cluster error"));

        // when
        List<BrokerInfoDTO> result = kafkaHealthService.getBrokerInfo();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("컨슈머 그룹 정보 조회 성공")
    void getAllConsumerGroups_ShouldReturnConsumerGroupList() {
        // given
        setupConsumerGroupMocks();

        // when
        List<ConsumerGroupInfoDTO> result = kafkaHealthService.getAllConsumerGroups();

        // then
        assertThat(result).hasSize(2);
        ConsumerGroupInfoDTO healthyGroup = result.stream()
                .filter(group -> group.getGroupId().equals("healthy-group"))
                .findFirst()
                .orElseThrow();

        assertThat(healthyGroup.isHealthy()).isTrue();
        assertThat(healthyGroup.getStatus()).isEqualTo("Stable");
    }

    @Test
    @DisplayName("컨슈머 그룹 조회 실패 시 빈 리스트 반환")
    void getAllConsumerGroups_ShouldReturnEmptyList_WhenListFails() {
        // given
        when(kafkaAdminClient.listConsumerGroups()).thenThrow(new RuntimeException("List error"));

        // when
        List<ConsumerGroupInfoDTO> result = kafkaHealthService.getAllConsumerGroups();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("컨슈머 그룹 상세 정보 조회 실패")
    void getAllConsumerGroups_ShouldHandleDescribeFailure() {
        // given
        ConsumerGroupListing listing = mock(ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn("failing-group");
        when(listing.isSimpleConsumerGroup()).thenReturn(false);

        when(kafkaAdminClient.listConsumerGroups()).thenReturn(consumerGroupsResult);
        doReturn(KafkaFuture.completedFuture(List.of(listing)))
                .when(consumerGroupsResult).all();

        when(kafkaAdminClient.describeConsumerGroups(Collections.singletonList("failing-group")))
                .thenThrow(new RuntimeException("Describe failed"));

        // when
        List<ConsumerGroupInfoDTO> result = kafkaHealthService.getAllConsumerGroups();

        // then
        assertThat(result).hasSize(1);
        ConsumerGroupInfoDTO group = result.get(0);
        assertThat(group.getGroupId()).isEqualTo("failing-group");
        assertThat(group.isHealthy()).isFalse();
        assertThat(group.getErrorMessage()).contains("Describe failed");
    }

    @ParameterizedTest
    @DisplayName("컨슈머 그룹 상태별 처리")
    @MethodSource("consumerGroupStatusScenarios")
    void getAllConsumerGroups_ShouldHandleDifferentStates(String groupId, ConsumerGroupState state, boolean expectedHealthy) {
        // given
        setupConsumerGroupWithState(groupId, state);

        // when
        List<ConsumerGroupInfoDTO> result = kafkaHealthService.getAllConsumerGroups();

        // then
        ConsumerGroupInfoDTO group = result.stream()
                .filter(g -> g.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow();

        assertThat(group.isHealthy()).isEqualTo(expectedHealthy);
        assertThat(group.getStatus()).isEqualTo(state.toString());
    }

    static Stream<Arguments> consumerGroupStatusScenarios() {
        return Stream.of(
                Arguments.of("stable-group", ConsumerGroupState.STABLE, true),
                Arguments.of("rebalancing-group", ConsumerGroupState.PREPARING_REBALANCE, false),
                Arguments.of("completing-group", ConsumerGroupState.COMPLETING_REBALANCE, false),
                Arguments.of("dead-group", ConsumerGroupState.DEAD, false)
        );
    }

    @Test
    @DisplayName("카프카 대시보드 정보 조회 성공")
    void getKafkaDashboard_ShouldReturnDashboardInfo() {
        // given
        setupHealthyKafkaMocks();

        // when
        KafkaDashboardDTO result = kafkaHealthService.getKafkaDashboard();

        // then
        assertThat(result.getHealth()).isNotNull();
        assertThat(result.getHealth().isHealthy()).isTrue();
        assertThat(result.getBootstrapServers()).isEqualTo(BOOTSTRAP_SERVERS);
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDltTopics()).isNotNull();
    }


    @Test
    @DisplayName("카프카 상태 모니터링 - 정상 상태")
    void monitorKafkaHealth_ShouldNotSendAlert_WhenHealthy() {
        // given
        setupHealthyKafkaMocks();

        // when
        kafkaHealthService.monitorKafkaHealth();

        // then
        verify(slackNotifier, never()).sendKafkaBrokerAlert(any(), any());
    }

    @Test
    @DisplayName("카프카 상태 모니터링 - 비정상 상태 시 알림 전송")
    void monitorKafkaHealth_ShouldSendAlert_WhenUnhealthy() {
        // given
        when(kafkaAdminClient.describeCluster()).thenThrow(new RuntimeException("Broker down"));

        // when
        kafkaHealthService.monitorKafkaHealth();

        // then
        verify(slackNotifier).sendKafkaBrokerAlert(
                eq(BOOTSTRAP_SERVERS),
                contains("브로커 연결 실패")
        );
    }

    @Test
    @DisplayName("카프카 상태 모니터링 - 예외 발생 시 로그 기록")
    void monitorKafkaHealth_ShouldLogException_WhenErrorOccurs() {
        // given
        KafkaHealthService spyService = spy(kafkaHealthService);
        doThrow(new RuntimeException("Monitoring error")).when(spyService).checkKafkaHealth();

        // when & then
        assertDoesNotThrow(() -> spyService.monitorKafkaHealth());
    }

    private void setupHealthyKafkaMocks() {
        setupBrokerInfoMocks();
        setupTopicMocks();
        setupConsumerGroupMocks();
    }

    private void setupBrokerInfoMocks() {
        when(kafkaAdminClient.describeCluster()).thenReturn(clusterResult);
        doReturn(KafkaFuture.completedFuture(brokerNodes))
                .when(clusterResult).nodes();
    }

    private void setupTopicMocks() {
        when(kafkaAdminClient.listTopics()).thenReturn(topicsResult);
        doReturn(KafkaFuture.completedFuture(topicNames))
                .when(topicsResult).names();
    }

    private void setupConsumerGroupMocks() {
        when(kafkaAdminClient.listConsumerGroups()).thenReturn(consumerGroupsResult);
        doReturn(KafkaFuture.completedFuture(consumerGroups))
                .when(consumerGroupsResult).all();

        for (ConsumerGroupListing listing : consumerGroups) {
            DescribeConsumerGroupsResult describeResult = mock(DescribeConsumerGroupsResult.class);
            ConsumerGroupDescription description = mock(ConsumerGroupDescription.class);

            when(kafkaAdminClient.describeConsumerGroups(Collections.singletonList(listing.groupId())))
                    .thenReturn(describeResult);

            doReturn(Map.of(listing.groupId(), KafkaFuture.completedFuture(description)))
                    .when(describeResult).describedGroups();

            when(description.state()).thenReturn(ConsumerGroupState.STABLE);
            when(description.members()).thenReturn(Collections.emptySet());

            ListConsumerGroupOffsetsResult offsetsResult = mock(ListConsumerGroupOffsetsResult.class);
            when(kafkaAdminClient.listConsumerGroupOffsets(listing.groupId())).thenReturn(offsetsResult);

            doReturn(KafkaFuture.completedFuture(Collections.emptyMap()))
                    .when(offsetsResult).partitionsToOffsetAndMetadata();
        }
    }

    private void setupConsumerGroupWithState(String groupId, ConsumerGroupState state) {
        ConsumerGroupListing listing = mock(ConsumerGroupListing.class);
        when(listing.groupId()).thenReturn(groupId);
        when(listing.isSimpleConsumerGroup()).thenReturn(false);

        when(kafkaAdminClient.listConsumerGroups()).thenReturn(consumerGroupsResult);
        doReturn(KafkaFuture.completedFuture(List.of(listing)))
                .when(consumerGroupsResult).all();

        DescribeConsumerGroupsResult describeResult = mock(DescribeConsumerGroupsResult.class);
        ConsumerGroupDescription description = mock(ConsumerGroupDescription.class);

        when(kafkaAdminClient.describeConsumerGroups(Collections.singletonList(groupId)))
                .thenReturn(describeResult);

        doReturn(Map.of(groupId, KafkaFuture.completedFuture(description)))
                .when(describeResult).describedGroups();

        when(description.state()).thenReturn(state);
        when(description.members()).thenReturn(Collections.emptySet());

        ListConsumerGroupOffsetsResult offsetsResult = mock(ListConsumerGroupOffsetsResult.class);
        when(kafkaAdminClient.listConsumerGroupOffsets(groupId)).thenReturn(offsetsResult);

        doReturn(KafkaFuture.completedFuture(Collections.emptyMap()))
                .when(offsetsResult).partitionsToOffsetAndMetadata();
    }

    private Collection<Node> createMockBrokerNodes() {
        List<Node> nodes = new ArrayList<>();
        for (int i = 1; i <= BROKER_COUNT; i++) {
            Node node = mock(Node.class);
            when(node.id()).thenReturn(i);
            when(node.host()).thenReturn("broker" + i);
            when(node.port()).thenReturn(9092);
            when(node.rack()).thenReturn("rack" + i);
            nodes.add(node);
        }
        return nodes;
    }

    private Set<String> createMockTopicNames() {
        Set<String> topics = new HashSet<>();
        for (int i = 1; i <= TOPIC_COUNT - DLT_TOPIC_COUNT; i++) {
            topics.add("topic" + i);
        }
        topics.add("user-events-dlt");
        topics.add("notification-events-dlt");
        return topics;
    }

    private Collection<ConsumerGroupListing> createMockConsumerGroups() {
        List<ConsumerGroupListing> groups = new ArrayList<>();

        ConsumerGroupListing healthyGroup = mock(ConsumerGroupListing.class);
        when(healthyGroup.groupId()).thenReturn("healthy-group");
        when(healthyGroup.isSimpleConsumerGroup()).thenReturn(false);
        groups.add(healthyGroup);

        ConsumerGroupListing unhealthyGroup = mock(ConsumerGroupListing.class);
        when(unhealthyGroup.groupId()).thenReturn("unhealthy-group");
        when(unhealthyGroup.isSimpleConsumerGroup()).thenReturn(false);
        groups.add(unhealthyGroup);

        return groups;
    }
}
