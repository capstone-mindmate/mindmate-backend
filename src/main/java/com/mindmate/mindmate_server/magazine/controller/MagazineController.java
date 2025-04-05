package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.magazine.dto.MagazineCreateRequest;
import com.mindmate.mindmate_server.magazine.dto.MagazineDetailResponse;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.dto.MagazineUpdateRequest;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/magazine")
public class MagazineController {
    private final MagazineService magazineService;
    /**
     * 메거진 작성
     */
    @PostMapping
    public ResponseEntity<MagazineResponse> createMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MagazineCreateRequest request) {
        MagazineResponse response = magazineService.createMagazine(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 메거진 수정
     */
    @PutMapping("/{magazineId}")
    public ResponseEntity<MagazineResponse> updateMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId,
            @Valid @RequestBody MagazineUpdateRequest request) {
        MagazineResponse response = magazineService.updateMagazine(magazineId, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 메거진 삭제
     */
    @DeleteMapping("/{magazineId}")
    public ResponseEntity<Void> deleteMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId) {
        magazineService.deleteMagazine(magazineId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 메거진 목록 조회
     * (카테고리 + 필터링?)
     * todo: querydsl로 여러 필터링 적용
     */
//    @GetMapping
//    public ResponseEntity<Page<MagazineResponse>> getMagazines(
//            @AuthenticationPrincipal UserPrincipal principal,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        Page<MagazineResponse> magazineResponses = magazineService.getMagazines(principal.getUserId(), PageRequest.of(page, size, Sort.by("createdAt").descending()));
//        return ResponseEntity.ok(magazineResponses);
//    }

    /**
     * 메거진 상세 조회
     */
    @GetMapping("/{magazineId}")
    public ResponseEntity<MagazineDetailResponse> getMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId) {
        MagazineDetailResponse response = magazineService.getMagazine(magazineId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 메거진 검색
     */

    /**
     * 인기 메거진
     */
}
