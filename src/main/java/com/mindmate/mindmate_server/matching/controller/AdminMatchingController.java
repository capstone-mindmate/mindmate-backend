package com.mindmate.mindmate_server.matching.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.dto.MatchingResponse;
import com.mindmate.mindmate_server.matching.service.AdminMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/matchings")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin Reviews", description = "관리자 매칭 관리 API")
public class AdminMatchingController {

    private final AdminMatchingService adminMatchingService;

    @GetMapping
    @Operation(summary = "매칭 목록 조회", description = "오픈된 모든 매칭을 조회합니다.")
    public ResponseEntity<Page<MatchingResponse>> getMatchings(
            @Parameter(description = "페이지네이션 정보")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            MatchingCategory matchingCategory){

        Page<MatchingResponse> matchings = adminMatchingService.getMatchings(pageable, matchingCategory);

        return ResponseEntity.ok(matchings);
    }

    @Operation(summary = "매칭 반려", description = "관리자가 매칭을 거절합니다.")
    @PatchMapping("/{matchingId}")
    public ResponseEntity<Void> rejectMatching(
            @PathVariable Long matchingId) {

        adminMatchingService.rejectMatching(matchingId);

        return ResponseEntity.ok().build();
    }

}
