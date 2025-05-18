package com.mindmate.mindmate_server.global.controller;

import com.mindmate.mindmate_server.global.dto.*;
import com.mindmate.mindmate_server.global.service.DltMonitoringService;
import com.mindmate.mindmate_server.global.service.DltReprocessingService;
import com.mindmate.mindmate_server.global.service.KafkaHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/kafka")
@RequiredArgsConstructor
@Tag(name = "Kafka 관리", description = "Kafka DLT 모니터링 및 관리 API")
public class KafkaAdminController {
    private final DltMonitoringService dltMonitoringService;
    private final DltReprocessingService dltReprocessingService;
    private final KafkaHealthService kafkaHealthService;

    /**
     * 모든 DLT 토픽 및 메시지 수 조회
     */
    @Operation(summary = "DLT 토픽 상태 조회", description = "모든 DLT 토픽과 각 토픽의 메시지 수를 조회합니다.")
    @GetMapping("/dlt")
    public ResponseEntity<List<DltTopicStatusDTO>> getDltStatus() {
        return ResponseEntity.ok(dltMonitoringService.getDltTopicStatus());
    }

    /**
     * 특정 DLT 토픽의 메시지 내용 조회
     */
    @Operation(summary = "DLT 메시지 내용 조회", description = "특정 DLT 토픽의 메시지 내용을 조회합니다.")
    @GetMapping("/dlt/{topic}")
    public ResponseEntity<List<DltMessageDTO>> getDltMessages(
            @PathVariable String topic,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dltMonitoringService.getDltMessages(topic, limit));
    }

    /**
     * 특정 컨슈머 그룹의 DLT 메시지 조회
     */
    @Operation(summary = "특정 컨슈머 그룹의 DLT 메시지 조회", description = "특정 컨슈머 그룹의 DLT 메시지를 조회합니다.")
    @GetMapping("/dlt/group/{groupId}/messages")
    public ResponseEntity<List<DltMessageDTO>> getDltMessagesForGroup(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dltMonitoringService.getDltMessagesForGroup(groupId, limit));
    }

    /**
     * DLT 메시지 재처리
     */
    @Operation(summary = "DLT 메시지 재처리", description = "DLT에 있는 메시지를 원본 토픽으로 재발행합니다.")
    @PostMapping("/dlt/{topic}/reprocess")
    public ResponseEntity<Map<String, Object>> reprocessDltMessages(
            @PathVariable String topic,
            @RequestParam(defaultValue = "100") int maxMessage) {
        // 컨슈머 그룹 정보 추출
        String consumerGroup = dltReprocessingService.extractConsumerGroupFromDltTopic(topic);
        int processedCount = dltReprocessingService.reprocessDltMessage(topic, maxMessage);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", String.format("DLT %s의 메시지 %d개가 재처리되었습니다 (컨슈머 그룹: %s).",
                        topic, processedCount, consumerGroup)
        ));
    }

    /**
     * 특정 컨슈머 그룹의 DLT 메시지 재처리
     */
    @Operation(summary = "특정 컨슈머 그룹의 DLT 메시지 재처리", description = "특정 컨슈머 그룹의 DLT 메시지를 원본 토픽으로 재발행합니다.")
    @PostMapping("/dlt/group/{groupId}/reprocess")
    public ResponseEntity<Map<String, Object>> reprocessDltMessagesForGroup(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "100") int maxMessage) {
        int processedCount = dltReprocessingService.reprocessDltForGroup(groupId, maxMessage);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", String.format("컨슈머 그룹 %s의 DLT 메시지 %d개가 재처리되었습니다.",
                        groupId, processedCount)
        ));
    }

    /**
     * 특정 컨슈머 그룹의 DLT 토픽 목록 조회
     */
    @Operation(summary = "특정 컨슈머 그룹의 DLT 토픽 목록 조회", description = "특정 컨슈머 그룹과 관련된 DLT 토픽 목록을 조회합니다.")
    @GetMapping("/dlt/group/{groupId}/topics")
    public ResponseEntity<List<String>> getDltTopicsForGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(dltReprocessingService.findDltTopicsForGroup(groupId));
    }

    /**
     * 카프카 토픽 목록 조회
     */
    @Operation(summary = "카프카 토픽 목록 조회", description = "모든 카프카 토픽 목록을 조회합니다.")
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getTopics() {
        return ResponseEntity.ok(dltMonitoringService.getAllTopics());
    }

    /**
     * DLT 메시지 삭제
     */
    @Operation(summary = "DLT 메시지 삭제", description = "특정 DLT 토픽의 모든 메시지를 삭제합니다.")
    @DeleteMapping("/dlt/{topic}")
    public ResponseEntity<Map<String, Object>> purgeDltMessages(@PathVariable String topic) {
        String consumerGroup = dltReprocessingService.extractConsumerGroupFromDltTopic(topic);
        dltReprocessingService.purgeDltMessages(topic);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", String.format("DLT %s의 메시지가 삭제되었습니다 (컨슈머 그룹: %s).",
                        topic, consumerGroup)
        ));
    }

    /**
     * 특정 컨슈머 그룹의 모든 DLT 메시지 삭제
     */
    @Operation(summary = "특정 컨슈머 그룹의 모든 DLT 메시지 삭제", description = "특정 컨슈머 그룹과 관련된 모든 DLT 토픽의 메시지를 삭제합니다.")
    @DeleteMapping("/dlt/group/{groupId}")
    public ResponseEntity<Map<String, Object>> purgeDltMessagesForGroup(@PathVariable String groupId) {
        List<String> dltTopics = dltReprocessingService.findDltTopicsForGroup(groupId);

        if (dltTopics.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", "warning",
                    "message", String.format("컨슈머 그룹 %s에 대한 DLT 토픽이 없습니다.", groupId)
            ));
        }

        for (String dltTopic : dltTopics) {
            dltReprocessingService.purgeDltMessages(dltTopic);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", String.format("컨슈머 그룹 %s의 모든 DLT 메시지가 삭제되었습니다 (토픽: %s).",
                        groupId, String.join(", ", dltTopics))
        ));
    }

    /**
     * 카프카 브로커 상태 조회
     */
    @Operation(summary = "카프카 브로커 상태 조회", description = "카프카 브로커의 상태를 조회합니다.")
    @GetMapping("/brokers")
    public ResponseEntity<List<BrokerInfoDTO>> getBrokers() {
        return ResponseEntity.ok(kafkaHealthService.getBrokerInfo());
    }

    /**
     * 카프카 상태 대시보드
     */
    @Operation(summary = "카프카 상태 대시보드", description = "카프카 전체 상태 정보를 조회합니다.")
    @GetMapping("/dashboard")
    public ResponseEntity<KafkaDashboardDTO> getDashboard() {
        return ResponseEntity.ok(kafkaHealthService.getKafkaDashboard());
    }

    /**
     * 카프카 상태 확인
     */
    @Operation(summary = "카프카 상태 확인", description = "카프카 브로커 연결 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkKafkaHealth() {
        KafkaHealthDTO health = kafkaHealthService.checkKafkaHealth();

        Map<String, Object> response = new HashMap<>();
        response.put("status", health.isHealthy() ? "UP" : "DOWN");
        response.put("timestamp", Instant.now().toString());
        response.put("details", health);

        return ResponseEntity.ok(response);
    }

    /**
     * 수동 DLT 모니터링 실행
     */
    @Operation(summary = "DLT 모니터링 수동 실행", description = "DLT 모니터링을 수동으로 실행합니다.")
    @PostMapping("/dlt/monitor")
    public ResponseEntity<Map<String, Object>> runDltMonitoring() {
        dltMonitoringService.monitorDltTopics();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "DLT 모니터링이 수동으로 실행되었습니다."
        ));
    }

    /**
     * 토픽의 메시지 수 조회
     */
    @Operation(summary = "토픽의 메시지 수 조회", description = "특정 토픽의 메시지 수를 조회합니다.")
    @GetMapping("/topics/{topic}/count")
    public ResponseEntity<Map<String, Object>> getTopicMessageCount(@PathVariable String topic) {
        long count = dltReprocessingService.getMessageCount(topic);
        return ResponseEntity.ok(Map.of(
                "topic", topic,
                "messageCount", count,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * 컨슈머 그룹 목록 조회
     */
    @Operation(summary = "컨슈머 그룹 목록 조회", description = "모든 컨슈머 그룹 목록을 조회합니다.")
    @GetMapping("/consumer-groups")
    public ResponseEntity<List<ConsumerGroupInfoDTO>> getConsumerGroups() {
        return ResponseEntity.ok(kafkaHealthService.getAllConsumerGroups());
    }
}
