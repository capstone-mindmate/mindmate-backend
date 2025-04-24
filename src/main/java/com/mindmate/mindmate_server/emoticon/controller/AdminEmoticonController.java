package com.mindmate.mindmate_server.emoticon.controller;

import com.mindmate.mindmate_server.emoticon.dto.EmoticonAdminResponse;
import com.mindmate.mindmate_server.emoticon.service.AdminEmoticonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/emoticons")
@RequiredArgsConstructor
public class AdminEmoticonController {
    private final AdminEmoticonService emoticonService;

    /**
     * 이모티콘 등록 요청 목록 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<List<EmoticonAdminResponse>> getPendingEmoticons() {
        List<EmoticonAdminResponse> pendingEmoticons = emoticonService.getPendingEmoticons();
        return ResponseEntity.ok(pendingEmoticons);
    }

    /**
     * 이모티콘 등록 요청 수락
     */
    @PostMapping("/{emoticonId}/accept")
    public ResponseEntity<Void> acceptEmoticon(@PathVariable Long emoticonId) {
        emoticonService.acceptEmoticon(emoticonId);
        return ResponseEntity.ok().build();
    }

    /**
     * 이모티콘 증록 요청 거절
     */
    @PostMapping("/{emoticonId}/reject")
    public ResponseEntity<Void> rejectEmoticon(@PathVariable Long emoticonId) {
        emoticonService.rejectEmoticon(emoticonId);
        return ResponseEntity.ok().build();
    }
}
