package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.dto.UserFilteringHistoryDTO;
import com.mindmate.mindmate_server.chat.service.AdminFilteringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "관리자 필터링 이력",
        description = "관리자가 채팅 필터링 이력을 조회할 수 있는 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/filtering")
public class AdminFilteringController {
    private final AdminFilteringService adminFilteringService;

    @Operation(
            summary = "사용자별 필터링 이력 조회",
            description = "특정 사용자의 최근 24시간 이내 채팅방별 필터링(욕설 등) 이력과 내용을 조회합니다."
    )
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserFilteringHistoryDTO> getUserFilteringHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(adminFilteringService.getUserFilteringHistory(userId));
    }

    @Operation(
            summary = "전체 사용자 필터링 이력 목록 조회",
            description = "최근 24시간 이내 필터링 이력이 있는 모든 사용자의 필터링 이력 목록을 조회합니다. " +
                    "사용자별 필터링 총 횟수 기준으로 내림차순 정렬됩니다."
    )
    @GetMapping("/users")
    public ResponseEntity<List<UserFilteringHistoryDTO>> getUserFilteringHistories() {
        return ResponseEntity.ok(adminFilteringService.getUserFilteringHistories());
    }
}
