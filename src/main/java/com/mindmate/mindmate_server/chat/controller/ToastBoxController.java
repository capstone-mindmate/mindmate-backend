package com.mindmate.mindmate_server.chat.controller;


import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordDTO;
import com.mindmate.mindmate_server.chat.dto.ToastBoxKeywordRequest;
import com.mindmate.mindmate_server.chat.service.ToastBoxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/toast-box")
@RequiredArgsConstructor
public class ToastBoxController {
    private final ToastBoxService toastBoxService;

    @GetMapping("/keywords")
    public ResponseEntity<List<ToastBoxKeywordDTO>> getAllKeywords() {
        List<ToastBoxKeywordDTO> keywords = toastBoxService.getAllToastBoxWords();
        return ResponseEntity.ok(keywords);
    }

    @PostMapping("/keywords")
    public ResponseEntity<ToastBoxKeywordDTO> addKeyword(@RequestBody ToastBoxKeywordRequest dto) {
        ToastBoxKeywordDTO word = toastBoxService.addToastBoxKeyword(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(word);
    }

    @PutMapping("/keywords/{id}")
    public ResponseEntity<ToastBoxKeywordDTO> updateKeyword(
            @PathVariable Long id, @RequestBody ToastBoxKeywordDTO dto) {
        ToastBoxKeywordDTO updated = toastBoxService.updateToastBoxKeyword(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/keywords/{id}")
    public ResponseEntity<Void> deleteKeyword(@PathVariable Long id) {
        toastBoxService.deleteToastBoxKeyWord(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/keywords/{id}/activate")
    public ResponseEntity<ToastBoxKeywordDTO> activateKeyword(@PathVariable Long id) {
        ToastBoxKeywordDTO activated = toastBoxService.setToastBoxKeywordActive(id, true);
        return ResponseEntity.ok(activated);
    }

    @PutMapping("/keywords/{id}/deactivate")
    public ResponseEntity<ToastBoxKeywordDTO> deactivateKeyword(@PathVariable Long id) {
        ToastBoxKeywordDTO deactivated = toastBoxService.setToastBoxKeywordActive(id, false);
        return ResponseEntity.ok(deactivated);
    }

    @PostMapping("/keywords/refresh")
    public ResponseEntity<Void> refreshToastBoxKeywords() {
        toastBoxService.refreshToastBoxKeywords();
        return ResponseEntity.noContent().build();
    }
}
