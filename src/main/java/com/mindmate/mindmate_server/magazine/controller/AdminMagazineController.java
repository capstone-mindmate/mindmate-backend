package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.magazine.dto.MagazineCategoryStatistics;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.magazine.service.MagazineStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "관리자 매거진 관리",
        description = "관리자가 매거진 등록 요청을 승인/거부하고, 통계 데이터를 조회할 수 있는 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/magazine")
public class AdminMagazineController {
    private final MagazineService magazineService;
    private final MagazineStatisticsService statisticsService;

    @Operation(
            summary = "승인 대기 매거진 목록 조회",
            description = "승인 대기(PENDING) 상태의 매거진 목록을 조회합니다."
    )
    @GetMapping("/pending")
    public ResponseEntity<Page<MagazineResponse>> getPendingMagazines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MagazineResponse> pendingMagazines = magazineService.getPendingMagazines(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        return ResponseEntity.ok(pendingMagazines);
    }

    @Operation(
            summary = "매거진 승인/거부 처리",
            description = "지정한 매거진을 승인(발행) 또는 거부 처리합니다. isAccepted=true면 승인, false면 거부."
    )
    @PostMapping("/{magazineId}")
    public ResponseEntity<MagazineResponse> publishMagazine(@PathVariable Long magazineId, @RequestParam boolean isAccepted) {
        return ResponseEntity.ok(magazineService.manageMagazine(magazineId, isAccepted));
    }


    @Operation(
            summary = "카테고리별 매거진 통계 조회",
            description = "카테고리별 매거진 등록/발행 통계 데이터를 조회합니다."
    )
    @GetMapping("/stats/category")
    public ResponseEntity<List<MagazineCategoryStatistics>> getCategoryStatistics() {
        return ResponseEntity.ok(statisticsService.getCategoryStatistics());
    }

    // todo: 추가 admin 매거진 관련 통계 데이터 추출

}