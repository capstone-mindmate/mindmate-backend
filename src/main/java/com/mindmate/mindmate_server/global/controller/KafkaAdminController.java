package com.mindmate.mindmate_server.global.controller;

import com.mindmate.mindmate_server.global.dto.DlqMessageDTO;
import com.mindmate.mindmate_server.global.dto.DlqTopicStatusDTO;
import com.mindmate.mindmate_server.global.entity.KafkaTopic;
import com.mindmate.mindmate_server.global.service.DlqMonitoringService;
import com.mindmate.mindmate_server.global.service.DlqReprocessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/kafka")
@RequiredArgsConstructor
@Tag(name = "Kafka 관리", description = "Kafka DLQ 모니터링 및 관리 API")
public class KafkaAdminController {
    private final DlqMonitoringService dlqMonitoringService;
    private final DlqReprocessingService dlqReprocessingService;

    /**
     * 모든 DLQ 토픽 및 메시지 수 조회
     */
    @Operation(summary = "DLQ 토픽 상태 조회", description = "모든 DLQ 토픽과 각 토픽의 메시지 수를 조회합니다.")
    @GetMapping("/dlq")
    public ResponseEntity<List<DlqTopicStatusDTO>> getDlqStatus() {
        return ResponseEntity.ok(dlqMonitoringService.getDlqTopicStatus());
    }

    /**
     * 특정 DLQ 토픽의 메시지 내용 조회
     */
    @Operation(summary = "DLQ 메시지 내용 조회", description = "특정 DLQ 토픽의 메시지 내용을 조회합니다.")
    @GetMapping("/dlq/{topic}")
    public ResponseEntity<List<DlqMessageDTO>> getDlqMessages(
            @PathVariable KafkaTopic topic,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dlqMonitoringService.getDlqMessages(topic.getDlqName(), limit));
    }

    /**
     * DLQ 메시지 재처리
     */
    @Operation(summary = "DLQ 메시지 재처리", description = "DLQ에 있는 메시지를 원본 토픽으로 재발행합니다.")
    @PostMapping("/dlq/{topic}/reprocess")
    public ResponseEntity<Map<String, Object>> reprocessDlqMessages(
            @PathVariable KafkaTopic topic,
            @RequestParam(defaultValue = "100") int maxMessage) {
        dlqReprocessingService.reprocessDlqMessage(topic.getDlqName(), maxMessage);
        return ResponseEntity.ok(Map.of(
                "status", "success", "message", String.format("DLQ %s의 메시지 재처리가 시작되었습니다.", topic)
        ));
    }

    /**
     * 카프카 토픽 목록 조회
     */
    @Operation(summary = "카프카 토픽 목록 조회", description = "모든 카프카 토픽 목록을 조회합니다.")
    @GetMapping("/topics")
    public ResponseEntity<List<String>> getTopics() {
        return ResponseEntity.ok(dlqMonitoringService.getAllTopics());
    }

    /**
     * DLQ 메시지 삭제
     */
    @DeleteMapping("/dlq/{topic}")
    public ResponseEntity<Map<String, Object>> purgeDlqMessages(@PathVariable KafkaTopic topic) {
        dlqReprocessingService.purgeDlqMessages(topic.getDlqName());
        return ResponseEntity.ok(Map.of(
                "status", "success", "message", String.format("DLQ %s의 메시지가 삭제되었스빈다..", topic)
        ));
    }
}
