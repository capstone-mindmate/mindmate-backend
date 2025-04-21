package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.report.dto.SuspensionRequest;
import com.mindmate.mindmate_server.report.dto.UnsuspendRequest;
import com.mindmate.mindmate_server.user.dto.SuspendedUserDTO;
import com.mindmate.mindmate_server.user.service.AdminUserSuspensionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserSuspensionController {
    private final AdminUserSuspensionService suspensionService;

    /**
     * 현재 정지된 모든 사용자 조회
     */
    @GetMapping("/suspended")
    public ResponseEntity<List<SuspendedUserDTO>> getSuspendedUsers() {
        return ResponseEntity.ok(suspensionService.getAllSuspendedUsers());
    }

    /**
     * 사용자 정지 처리
     */
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(
            @PathVariable Long userId,
            @RequestBody SuspensionRequest request) {
        suspensionService.suspendUser(userId, request.getReportCount(), request.getDuration(), request.getReason());
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 정지 해제
     */
    @PostMapping("/{userId}/unsuspend")
    public ResponseEntity<Void> unsuspendUser(
            @PathVariable Long userId,
            @RequestBody UnsuspendRequest request) {
        suspensionService.unsuspendUser(userId, request);
        return ResponseEntity.noContent().build();
    }
}
