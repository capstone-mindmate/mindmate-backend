package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        Page<MagazineResponse> magazineResponses = magazineService.getMagazines(principal.getUserId(), filter, PageRequest.of(page, size));
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
     * 좋아요 토글
     */
    @PostMapping("/{magazineId}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId) {
        LikeResponse response = magazineService.toggleLike(magazineId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 활동 정보 얻기 -> 사용자가 해당 매거진 조회 이탈 시 호출
     */
    @PostMapping("/{magazineId}/engagement")
    public ResponseEntity<Void> trackEngagement(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId,
            @RequestBody MagazineEngagementRequest request) {

        magazineService.handleEngagement(principal.getUserId(), magazineId, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * 인기 메거진
     */
    @GetMapping("/popular")
    public ResponseEntity<List<MagazineResponse>> getPopularMagazines(
            @RequestParam(defaultValue = "10") int limit) {
        List<MagazineResponse> popularMagazines = magazineService.getPopularMagazines(limit);
        return ResponseEntity.ok(popularMagazines);
    }

    /**
     * 카테고리별 인기 매거진 조회
     */
    @GetMapping("/popular/category/{category}")
    public ResponseEntity<List<MagazineResponse>> getPopularMagazinesByCategory (
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        List<MagazineResponse> popularMagazines = magazineService.getPopularMagazinesByCategory(category, limit);
        return ResponseEntity.ok(popularMagazines);
    }

}
