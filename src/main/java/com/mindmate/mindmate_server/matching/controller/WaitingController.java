package com.mindmate.mindmate_server.matching.controller;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.dto.ListenerStatus;
import com.mindmate.mindmate_server.matching.dto.ListenerStatusUpdateRequest;
import com.mindmate.mindmate_server.matching.dto.SpeakerStatusUpdateRequest;
import com.mindmate.mindmate_server.matching.dto.WaitingProfile;
import com.mindmate.mindmate_server.matching.service.WaitingService;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/waiting")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Waiting", description = "대기 상태 관리 관련 API")
public class WaitingController {
    private final WaitingService waitingService;

    @PutMapping("/listener/{listenerId}/status")
    @Operation(summary = "리스너 상태 업데이트", description = "리스너의 현재 상태를 업데이트합니다.")
    public ResponseEntity<Void> updateListenerStatus(
            @PathVariable Long listenerId,
            @Valid @RequestBody ListenerStatusUpdateRequest request) {

        waitingService.updateListenerStatus(listenerId, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/speaker/{speakerId}/status")
    @Operation(summary = "스피커 대기 상태 업데이트", description = "스피커가 매칭 대기 상태로 전환하거나 취소합니다.")
    public ResponseEntity<Void> updateSpeakerStatus(
            @PathVariable Long speakerId,
            @Valid @RequestBody SpeakerStatusUpdateRequest request) {

        waitingService.updateSpeakerStatus(
                speakerId,
                request.isAvailable(),
                request.getPreferredFields(),
                request.getPreferredStyle());
        return ResponseEntity.ok().build();
    }
    @GetMapping("/listeners")
    @Operation(summary = "대기 중인 리스너 목록 조회", description = "상담 가능한 리스너 목록을 조회합니다.")
    public ResponseEntity<List<WaitingProfile>> getWaitingListeners() {
        List<WaitingProfile> listeners = waitingService.getWaitingListeners();
        return ResponseEntity.ok(listeners);
    }

    @GetMapping("/speakers")
    @Operation(summary = "대기 중인 스피커 목록 조회", description = "매칭 대기 중인 스피커 목록을 조회합니다.")
    public ResponseEntity<List<WaitingProfile>> getWaitingSpeakers() {
        List<WaitingProfile> speakers = waitingService.getWaitingSpeakers();
        return ResponseEntity.ok(speakers);
    }

    @GetMapping("/status/{profileId}")
    @Operation(summary = "사용자 대기 상태 확인", description = "사용자가 현재 대기 중인지 확인합니다.")
    public ResponseEntity<Boolean> checkWaitingStatus(
            @PathVariable Long profileId,
            @RequestParam InitiatorType userType) {

        boolean isWaiting = waitingService.isUserWaiting(profileId, userType);
        return ResponseEntity.ok(isWaiting);
    }
}