package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.dto.FilteringWordDTO;
import com.mindmate.mindmate_server.chat.dto.FilteringWordRequest;
import com.mindmate.mindmate_server.chat.service.ContentFilterService;
import com.mindmate.mindmate_server.chat.service.FilteringWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "필터링 단어 관리",
        description = "관리자가 채팅 등에서 사용할 필터링(금칙어) 단어를 등록, 조회, 삭제, 활성/비활성, 갱신할 수 있는 API"
)
@RestController
@RequestMapping("/admin/filtering")
@RequiredArgsConstructor
public class FilteringWordController {
    private final FilteringWordService filteringWordService;
    private final ContentFilterService contentFilterService;

    @Operation(
            summary = "필터링 단어 전체 조회",
            description = "등록된 모든 필터링(금칙어) 단어 목록을 조회합니다."
    )
    @GetMapping("/words")
    public ResponseEntity<List<FilteringWordDTO>> getAllFilteringWords() {
        List<FilteringWordDTO> words = filteringWordService.getAllFilteringWords();
        return ResponseEntity.ok(words);
    }

    @Operation(
            summary = "필터링 단어 추가",
            description = "새로운 필터링(금칙어) 단어를 추가합니다."
    )
    @PostMapping("/words")
    public ResponseEntity<FilteringWordDTO> addFilteringWord(@RequestBody FilteringWordRequest request) {
        FilteringWordDTO saved = filteringWordService.addFilteringWord(request.getWord());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(
            summary = "필터링 단어 삭제",
            description = "지정한 필터링(금칙어) 단어를 삭제합니다."
    )
    @DeleteMapping("/words/{id}")
    public ResponseEntity<Void> deleteFilteringWord(@PathVariable Long id) {
        filteringWordService.deleteFilteringWord(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "필터링 단어 활성화",
            description = "지정한 필터링(금칙어) 단어를 활성화(사용 가능) 상태로 변경합니다."
    )
    @PutMapping("/words/{id}/activate")
    public ResponseEntity<FilteringWordDTO> activateFilteringWord(@PathVariable Long id) {
        FilteringWordDTO updated = filteringWordService.setFilteringWordActive(id, true);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "필터링 단어 비활성화",
            description = "지정한 필터링(금칙어) 단어를 비활성화(사용 불가) 상태로 변경합니다."
    )
    @PutMapping("/words/{id}/deactivate")
    public ResponseEntity<FilteringWordDTO> deactivateFilteringWord(@PathVariable Long id) {
        FilteringWordDTO updated = filteringWordService.setFilteringWordActive(id, false);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "필터링 단어 목록 갱신",
            description = "필터링(금칙어) 단어 목록을 즉시 갱신합니다. " +
                    "(예: 단어 추가/삭제/상태 변경 후 적용. 현재 수정하더라도 바로 업데이트 x. 특정 시간마다 스케줄링 )"
    )
    @PostMapping("/words/refresh")
    public ResponseEntity<Void> refreshToastBoxKeywords() {
        contentFilterService.refreshFilteringWords();
        return ResponseEntity.noContent().build();
    }
}

