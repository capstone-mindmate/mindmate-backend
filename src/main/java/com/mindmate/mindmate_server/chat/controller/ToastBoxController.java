package com.mindmate.mindmate_server.chat.controller;


import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordDTO;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordRequest;
import com.mindmate.mindmate_server.chat.service.ToastBoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "토스트박스 키워드 관리",
        description = "관리자가 토스트박스(알림/가이드) 키워드를 등록, 수정, 삭제, 활성/비활성, 갱신할 수 있는 API"
)
@RestController
@RequestMapping("/admin/toast-box")
@RequiredArgsConstructor
public class ToastBoxController {
    private final ToastBoxService toastBoxService;

    @Operation(
            summary = "토스트박스 키워드 전체 조회",
            description = "등록된 모든 토스트박스 키워드 목록을 조회합니다."
    )
    @GetMapping("/keywords")
    public ResponseEntity<List<ToastBoxKeywordDTO>> getAllKeywords() {
        List<ToastBoxKeywordDTO> keywords = toastBoxService.getAllToastBoxWords();
        return ResponseEntity.ok(keywords);
    }

    @Operation(
            summary = "토스트박스 키워드 추가",
            description = "새로운 토스트박스 키워드를 등록합니다."
    )
    @PostMapping("/keywords")
    public ResponseEntity<ToastBoxKeywordDTO> addKeyword(@RequestBody ToastBoxKeywordRequest dto) {
        ToastBoxKeywordDTO word = toastBoxService.addToastBoxKeyword(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(word);
    }

    @Operation(
            summary = "토스트박스 키워드 수정",
            description = "지정한 토스트박스 키워드의 정보를 수정합니다."
    )
    @PutMapping("/keywords/{id}")
    public ResponseEntity<ToastBoxKeywordDTO> updateKeyword(
            @PathVariable Long id, @RequestBody ToastBoxKeywordDTO dto) {
        ToastBoxKeywordDTO updated = toastBoxService.updateToastBoxKeyword(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "토스트박스 키워드 삭제",
            description = "지정한 토스트박스 키워드를 삭제합니다."
    )
    @DeleteMapping("/keywords/{id}")
    public ResponseEntity<Void> deleteKeyword(@PathVariable Long id) {
        toastBoxService.deleteToastBoxKeyWord(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "토스트박스 키워드 활성화",
            description = "지정한 토스트박스 키워드를 활성화(사용 가능) 상태로 변경합니다."
    )
    @PutMapping("/keywords/{id}/activate")
    public ResponseEntity<ToastBoxKeywordDTO> activateKeyword(@PathVariable Long id) {
        ToastBoxKeywordDTO activated = toastBoxService.setToastBoxKeywordActive(id, true);
        return ResponseEntity.ok(activated);
    }

    @Operation(
            summary = "토스트박스 키워드 비활성화",
            description = "지정한 토스트박스 키워드를 비활성화(사용 불가) 상태로 변경합니다."
    )
    @PutMapping("/keywords/{id}/deactivate")
    public ResponseEntity<ToastBoxKeywordDTO> deactivateKeyword(@PathVariable Long id) {
        ToastBoxKeywordDTO deactivated = toastBoxService.setToastBoxKeywordActive(id, false);
        return ResponseEntity.ok(deactivated);
    }

    @Operation(
            summary = "토스트박스 키워드 목록 갱신",
            description = "토스트박스 키워드 목록을 즉시 갱신합니다. (예: 키워드 추가/삭제/상태 변경 후 적용)"
    )
    @PostMapping("/keywords/refresh")
    public ResponseEntity<Void> refreshToastBoxKeywords() {
        toastBoxService.refreshToastBoxKeywords();
        return ResponseEntity.noContent().build();
    }
}
