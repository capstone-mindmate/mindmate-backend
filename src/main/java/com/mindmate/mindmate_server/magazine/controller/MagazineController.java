package com.mindmate.mindmate_server.magazine.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "매거진",
        description = "매거진(콘텐츠) 작성, 수정, 삭제, 목록/상세 조회, 좋아요, 인기 매거진 등 사용자용 매거진 관련 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/magazines")
public class MagazineController {
    private final MagazineService magazineService;

    @Operation(
            summary = "매거진 작성",
            description = "새로운 매거진(콘텐츠)을 작성합니다."
    )
    @PostMapping
    public ResponseEntity<MagazineResponse> createMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MagazineCreateRequest request) {
        MagazineResponse response = magazineService.createMagazine(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "매거진 수정",
            description = "기존 매거진(콘텐츠)을 수정합니다."
    )
    @PutMapping("/{magazineId}")
    public ResponseEntity<MagazineResponse> updateMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId,
            @Valid @RequestBody MagazineUpdateRequest request) {
        MagazineResponse response = magazineService.updateMagazine(magazineId, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "매거진 삭제",
            description = "지정한 매거진(콘텐츠)을 삭제합니다."
    )
    @DeleteMapping("/{magazineId}")
    public ResponseEntity<Void> deleteMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId) {
        magazineService.deleteMagazine(magazineId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "매거진 목록 조회",
            description = """
                매거진(콘텐츠) 목록을 조회합니다.
                - 카테고리, 키워드, 인기순/최신순 등 다양한 조건으로 필터링 및 검색이 가능합니다.
                - 아무 parameter를 안보내면 모드 카테고리, 최신순 조회
            """
    )
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


    @Operation(
            summary = "매거진 상세 조회",
            description = "지정한 매거진(콘텐츠)의 상세 정보를 조회합니다."
    )
    @GetMapping("/{magazineId}")
    public ResponseEntity<MagazineDetailResponse> getMagazine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId) {
        MagazineDetailResponse response = magazineService.getMagazine(magazineId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "매거진 좋아요 토글",
            description = "매거진(콘텐츠)에 좋아요를 추가하거나 취소합니다."
    )
    @PostMapping("/{magazineId}/like")
    public ResponseEntity<LikeResponse> toggleLike(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId) {
        LikeResponse response = magazineService.toggleLike(magazineId, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "매거진 활동 정보 기록",
            description = """
                - 매거진(콘텐츠) 조회 시 체류 시간, 스크롤 등 사용자 활동 정보를 기록합니다.
                - 사용자가 일정 시간 이상 매거진에 체류하면 해당 매거진을 벗어날 때 해당 api 보내기
        """
    )
    @PostMapping("/{magazineId}/engagement")
    public ResponseEntity<Void> trackEngagement(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long magazineId,
            @RequestBody MagazineEngagementRequest request) {

        magazineService.handleEngagement(principal.getUserId(), magazineId, request);
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "인기 매거진 목록 조회",
            description = "전체 인기 매거진(콘텐츠) 목록을 조회합니다."
    )
    @GetMapping("/popular")
    public ResponseEntity<List<MagazineResponse>> getPopularMagazines(
            @RequestParam(defaultValue = "10") int limit) {
        List<MagazineResponse> popularMagazines = magazineService.getPopularMagazines(limit);
        return ResponseEntity.ok(popularMagazines);
    }

    @Operation(
            summary = "카테고리별 인기 매거진 목록 조회",
            description = "지정한 카테고리의 인기 매거진(콘텐츠) 목록을 조회합니다."
    )
    @GetMapping("/popular/category/{category}")
    public ResponseEntity<List<MagazineResponse>> getPopularMagazinesByCategory (
            @PathVariable MatchingCategory category,
            @RequestParam(defaultValue = "10") int limit) {
        List<MagazineResponse> popularMagazines = magazineService.getPopularMagazinesByCategory(category, limit);
        return ResponseEntity.ok(popularMagazines);
    }

    @Operation(
            summary = "내가 작성한 매거진 목록 조회",
            description = "사용자가 작성한 매거진 목록을 최신순으로 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<Page<MagazineResponse>> getMyMagazines(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<MagazineResponse> myMagazines = magazineService.getMyMagazines(principal.getUserId(), PageRequest.of(page, size));

        return ResponseEntity.ok(myMagazines);
    }

    @Operation(
            summary = "좋아요 누른 매거진 목록 조회",
            description = "사용자가 좋아요를 누른 매거진 목록을 최신순으로 조회합니다."
    )
    @GetMapping("/liked")
    public ResponseEntity<Page<MagazineResponse>> getLikedMagazines(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<MagazineResponse> likedMagazines = magazineService.getLikedMagazines(principal.getUserId(), PageRequest.of(page, size));

        return ResponseEntity.ok(likedMagazines);
    }
}
