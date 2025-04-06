package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.magazine.dto.MagazineCategoryStatistics;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.magazine.service.MagazineStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/magazine")
public class AdminMagazineController {
    private final MagazineService magazineService;
    private final MagazineStatisticsService statisticsService;

    /**
     * 요청된 매거진 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<MagazineResponse>> getPendingMagazines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MagazineResponse> pendingMagazines = magazineService.getPendingMagazines(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(pendingMagazines);
    }

    /**
     * 매거진 승인/거부
     */
    @PostMapping("/{magazineId}")
    public ResponseEntity<MagazineResponse> publishMagazine(@PathVariable Long magazineId, @RequestParam boolean isAccepted) {
        return ResponseEntity.ok(magazineService.manageMagazine(magazineId, isAccepted));
    }

    /**
     * 카테고리별 매거진 통계
     */
    @GetMapping("/stats/category")
    public ResponseEntity<List<MagazineCategoryStatistics>> getCategoryStatistics() {
        return ResponseEntity.ok(statisticsService.getCategoryStatistics());
    }

    // todo: 추가 admin 매거진 관련 통계 데이터 추출

}