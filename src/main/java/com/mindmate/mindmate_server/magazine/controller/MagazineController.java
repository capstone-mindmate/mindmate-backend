package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
     * 1. 카테고리별 필터링
     * 2. 키워드 검색
     * 3. 인기순/생성순에 따라 필터링
     * 1, 2, 3을 혼합해서 호출 가능
     */
    @GetMapping
    public ResponseEntity<Page<MagazineResponse>> getMagazines(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) MatchingCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") MagazineSearchFilter.SortType sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        MagazineSearchFilter filter = MagazineSearchFilter.builder()
                .category(category)
                .keyword(keyword)
                .sortBy(sortBy)
                .build();

        Page<MagazineResponse> magazineResponses = magazineService.getMagazines(principal.getUserId(), filter, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(magazineResponses);
    }

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
     * 인기 메거진
     */
}
