package com.mindmate.mindmate_server.user.controller;

import com.mindmate.mindmate_server.report.dto.SuspensionRequest;
import com.mindmate.mindmate_server.report.dto.UnsuspendRequest;
import com.mindmate.mindmate_server.user.dto.SuspendedUserDTO;
import com.mindmate.mindmate_server.user.service.AdminUserSuspensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "관리자 사용자 정지 관리",
        description = "관리자가 사용자 정지/해제 및 정지된 사용자 목록을 관리하는 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserSuspensionController {
    private final AdminUserSuspensionService suspensionService;

    @Operation(
            summary = "정지된 사용자 전체 조회",
            description = """
                현재 정지(사용 중지) 상태인 모든 사용자 목록을 조회합니다.
            """
    )
    @GetMapping("/suspended")
    public ResponseEntity<List<SuspendedUserDTO>> getSuspendedUsers() {
        return ResponseEntity.ok(suspensionService.getAllSuspendedUsers());
    }


    @Operation(
            summary = "사용자 정지 처리",
            description = """
                지정한 사용자를 일정 기간 동안 정지(사용 중지) 상태로 만듭니다.
                - 정지 사유, 기간, 누적 신고 횟수 등 입력 가능
            """
    )
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(
            @PathVariable Long userId,
            @RequestBody SuspensionRequest request) {
        suspensionService.suspendUser(userId, request.getReportCount(), request.getDuration(), request.getReason());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "사용자 정지 해제",
            description = """
                정지 상태인 사용자를 즉시 정지 해제(복원)합니다.
                - 신고 횟수도 함께 조정 가능
            """
    )
    @PostMapping("/{userId}/unsuspend")
    public ResponseEntity<Void> unsuspendUser(
            @PathVariable Long userId,
            @RequestBody UnsuspendRequest request) {
        suspensionService.unsuspendUser(userId, request);
        return ResponseEntity.noContent().build();
    }
}
