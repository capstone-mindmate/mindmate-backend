package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/magazine")
public class AdminMagazineController {
    private final MagazineService magazineService;

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
     * 매거진 승인
     */
    @PostMapping("/{magazineId}")
    public ResponseEntity<MagazineResponse> publishMagazine(@PathVariable Long magazineId) {
        return ResponseEntity.ok(magazineService.publishMagazine(magazineId));
    }

    /**
     * 매거진 거부
     */
    @PostMapping("/{magazineId}")
    public ResponseEntity<MagazineResponse> rejectMagazine(@PathVariable Long magazineId) {
        return ResponseEntity.ok(magazineService.rejectMagazine(magazineId));
    }

}
