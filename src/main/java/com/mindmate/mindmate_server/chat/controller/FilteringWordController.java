package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.dto.FilteringWordDTO;
import com.mindmate.mindmate_server.chat.dto.FilteringWordRequest;
import com.mindmate.mindmate_server.chat.service.ContentFilterService;
import com.mindmate.mindmate_server.chat.service.FilteringWordService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/filtering")
@RequiredArgsConstructor
public class FilteringWordController {
    private final FilteringWordService filteringWordService;
    private final ContentFilterService contentFilterService;

    /**
     * 필터링 단어들 확인
     */
    @GetMapping("/words")
    public ResponseEntity<List<FilteringWordDTO>> getAllFilteringWords() {
        List<FilteringWordDTO> words = filteringWordService.getAllFilteringWords();
        return ResponseEntity.ok(words);
    }

    /**
     * 필터링 단어 추가
     */
    @PostMapping("/words")
    public ResponseEntity<FilteringWordDTO> addFilteringWord(@RequestBody FilteringWordRequest request) {
        try {
            FilteringWordDTO saved = filteringWordService.addFilteringWord(request.getWord());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

    }

    /**
     * 필터링 단어 삭제
     */
    @DeleteMapping("/words/{id}")
    public ResponseEntity<Void> deleteFilteringWord(@PathVariable Long id) {
        try {
            filteringWordService.deleteFilteringWord(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 필터링 단어 유효/무효
     */
    @PutMapping("/words/{id}/activate")
    public ResponseEntity<FilteringWordDTO> activateFilteringWord(@PathVariable Long id) {
        try {
            FilteringWordDTO updated = filteringWordService.activateFilteringWod(id);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/words/{id}/deactivate")
    public ResponseEntity<FilteringWordDTO> deactivateFilteringWord(@PathVariable Long id) {
        try {
            FilteringWordDTO updated = filteringWordService.deactivateFilteringWod(id);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

}

