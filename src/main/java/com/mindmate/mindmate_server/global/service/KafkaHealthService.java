package com.mindmate.mindmate_server.global.service;

import com.mindmate.mindmate_server.global.dto.*;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaHealthService {
    private final KafkaAdminClient kafkaAdminClient;
    private final SlackNotifier slackNotifier;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Scheduled(fixedRate = 3000000)
    public void monitorKafkaHealth() {
        try {
            KafkaHealthDTO health = checkKafkaHealth();

            if (!health.isHealthy()) {
                slackNotifier.sendKafkaBrokerAlert(
                        bootstrapServers,
                        "브로커 연결 실패: " + health.getErrorMessage()
                );
            }

            List<ConsumerGroupInfoDTO> unhealthyGroups = health.getConsumerGroups()
                    .stream()
                    .filter(group -> !group.isHealthy())
                    .collect(Collectors.toList());

            for (ConsumerGroupInfoDTO group : unhealthyGroups) {
                log.error("컨슈머 그룹 에러 발생");
//                slackNotifier.sendConsumerGroupAlert(
//                        group.getGroupId(),
//                        String.join(", ", group.getSubscribedTopics()),
//                        group.getStatus() + ": " + group.getErrorMessage()
//                );
            }
        } catch (Exception e) {
            log.error("카프카 상태 모니터링 중 오류 발생", e);
        }
    }

    public KafkaHealthDTO checkKafkaHealth() {
        KafkaHealthDTO health = new KafkaHealthDTO();
        try {
            DescribeClusterResult clusterResult = kafkaAdminClient.describeCluster();
            Collection<Node> nodes = clusterResult.nodes().get(5, TimeUnit.SECONDS);

            health.setHealthy(true);
            health.setBrokerCount(nodes.size());
            health.setBrokers(getBrokerInfo());

            health.setConsumerGroups(getAllConsumerGroups());
            health.setTopicCount(kafkaAdminClient.listTopics().names().get().size());

            long dltCount = kafkaAdminClient.listTopics().names().get().stream()
                    .filter(topic -> topic.endsWith("-dlt"))
                    .count();
            health.setDltTopicCount((int) dltCount);
        } catch (Exception e) {
            health.setHealthy(false);
            health.setErrorMessage(e.getMessage());
            log.error("카프카 상태 확인 실패: {}", e.getMessage());
        }
        return health;
    }


    public List<BrokerInfoDTO> getBrokerInfo() {
        List<BrokerInfoDTO> brokers = new ArrayList<>();
        try {
            DescribeClusterResult clusterResult = kafkaAdminClient.describeCluster();
            Collection<Node> nodes = clusterResult.nodes().get(5, TimeUnit.SECONDS);

            for (Node node : nodes) {
                BrokerInfoDTO broker = BrokerInfoDTO.builder()
                        .id(node.id())
                        .host(node.host())
                        .port(node.port())
                        .rack(node.rack())
                        .build();

                brokers.add(broker);
            }
        } catch (Exception e) {
            log.error("브로커 정보 조회 실패: {}", e.getMessage());
        }
        return brokers;
    }

    public List<ConsumerGroupInfoDTO> getAllConsumerGroups() {
        List<ConsumerGroupInfoDTO> groups = new ArrayList<>();

        try {
            ListConsumerGroupsResult listResult = kafkaAdminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groupListings = listResult.all().get();

            for (ConsumerGroupListing listing : groupListings) {
                ConsumerGroupInfoDTO group = new ConsumerGroupInfoDTO();
                group.setGroupId(listing.groupId());
                group.setSimple(listing.isSimpleConsumerGroup());

                try {
                    DescribeConsumerGroupsResult describeResult = kafkaAdminClient.describeConsumerGroups(Collections.singletonList(listing.groupId()));
                    ConsumerGroupDescription description = describeResult.describedGroups().get(listing.groupId()).get();
                    group.setStatus(description.state().toString());
                    group.setHealthy(description.state() == ConsumerGroupState.STABLE);

                    Set<String> topics = new HashSet<>();
                    for (MemberDescription member : description.members()) {
                        MemberAssignment assignment = member.assignment();
                        if (assignment != null) {
                            assignment.topicPartitions().forEach(tp -> topics.add(tp.topic()));
                        }
                    }
                    group.setSubscribedTopics(new ArrayList<>(topics));
                    Map<TopicPartition, OffsetAndMetadata> offsets = kafkaAdminClient.listConsumerGroupOffsets(listing.groupId())
                            .partitionsToOffsetAndMetadata().get();
                    group.setPartitionCount(offsets.size());
                } catch (Exception e) {
                    group.setHealthy(false);
                    group.setErrorMessage(e.getMessage());
                    log.warn("컨슈머 그룹 상세 정보 조회 실패: {}", e.getMessage());
                }

                groups.add(group);
            }
        } catch (Exception e) {
            log.error("컨슈머 그룹 목록 조회 실패: {}", e.getMessage());
        }
        return groups;
    }

    /**
     * 카프카 대시보드 정보 조회
     */
    public KafkaDashboardDTO getKafkaDashboard() {
        KafkaDashboardDTO dashboard = new KafkaDashboardDTO();

        try {
            KafkaHealthDTO health = checkKafkaHealth();
            dashboard.setHealth(health);

            // DLT 토픽 상태 정보
            List<DltTopicStatusDTO> dltStatus = new ArrayList<>();
            Set<String> dltTopics = kafkaAdminClient.listTopics().names().get().stream()
                    .filter(topic -> topic.endsWith("-dlt"))
                    .collect(Collectors.toSet());

            for (String dltTopic : dltTopics) {
                // DLT 토픽 상태 정보 수집 로직
                // 필요시 구현
            }

            dashboard.setDltTopics(dltStatus);

            // 시스템 정보
            dashboard.setBootstrapServers(bootstrapServers);
            dashboard.setTimestamp(LocalDateTime.now());

        } catch (Exception e) {
            dashboard.setError(e.getMessage());
            log.error("카프카 대시보드 정보 조회 실패: {}", e.getMessage());
        }

        return dashboard;
    }
}
