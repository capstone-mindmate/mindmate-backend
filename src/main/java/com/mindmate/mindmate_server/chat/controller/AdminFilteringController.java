package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.dto.UserFilteringHistoryDTO;
import com.mindmate.mindmate_server.chat.service.AdminFilteringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/filtering")
public class AdminFilteringController {
    private final AdminFilteringService adminFilteringService;

    /**
     * 사용자별 필터링 이력 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserFilteringHistoryDTO> getUserFilteringHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(adminFilteringService.getUserFilteringHistory(userId));
    }

    /**
     * 전체 필터링 이력 조회
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserFilteringHistoryDTO>> getUserFilteringHistories() {
        return ResponseEntity.ok(adminFilteringService.getUserFilteringHistories());
    }
}
