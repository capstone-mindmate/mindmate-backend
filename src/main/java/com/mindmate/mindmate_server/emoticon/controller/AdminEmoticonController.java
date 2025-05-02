package com.mindmate.mindmate_server.emoticon.controller;

import com.mindmate.mindmate_server.emoticon.dto.EmoticonAdminResponse;
import com.mindmate.mindmate_server.emoticon.service.AdminEmoticonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "이모티콘 등록 관리",
        description = "관리자가 이모티콘 등록 요청을 승인 또는 거절할 수 있는 API"
)
@RestController
@RequestMapping("/admin/emoticons")
@RequiredArgsConstructor
public class AdminEmoticonController {
    private final AdminEmoticonService emoticonService;

    @Operation(
            summary = "이모티콘 등록 요청 목록 조회",
            description = "승인 대기 중(PENDING)인 이모티콘 등록 요청 목록을 조회합니다."
    )
    @GetMapping("/pending")
    public ResponseEntity<List<EmoticonAdminResponse>> getPendingEmoticons() {
        List<EmoticonAdminResponse> pendingEmoticons = emoticonService.getPendingEmoticons();
        return ResponseEntity.ok(pendingEmoticons);
    }

    @Operation(
            summary = "이모티콘 등록 요청 수락",
            description = "지정한 이모티콘 등록 요청을 승인(ACCEPT) 처리합니다."
    )
    @PostMapping("/{emoticonId}/accept")
    public ResponseEntity<Void> acceptEmoticon(@PathVariable Long emoticonId) {
        emoticonService.acceptEmoticon(emoticonId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "이모티콘 등록 요청 거절",
            description = "지정한 이모티콘 등록 요청을 거절(REJECT) 처리합니다."
    )
    @PostMapping("/{emoticonId}/reject")
    public ResponseEntity<Void> rejectEmoticon(@PathVariable Long emoticonId) {
        emoticonService.rejectEmoticon(emoticonId);
        return ResponseEntity.ok().build();
    }
}
