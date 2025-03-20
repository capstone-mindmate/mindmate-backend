package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.MessageReactionResponse;
import com.mindmate.mindmate_server.chat.dto.ReactionRequest;
import com.mindmate.mindmate_server.chat.service.MessageReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class MessageInteractionController {
    private final MessageReactionService reactionService;

    /**
     * 메시지에 감정표현 추가
     * 이미 있으면 그냥 값 변경
     */
    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<MessageReactionResponse> addReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId,
            @RequestBody @Valid ReactionRequest request) {
        // todo: dto 수정 or PathVariable 수정 -> 지금 중복 값임
        MessageReactionResponse response = reactionService.addReaction(principal.getUserId(), messageId, request.getReactionType());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 메시지에 달린 모든 감정표현 조회
     */
    @GetMapping("/messages/{messageId}/reactions")
    public ResponseEntity<List<MessageReactionResponse>> getReactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId) {
        List<MessageReactionResponse> reactionResponses = reactionService.getReactions(messageId);
        return ResponseEntity.ok(reactionResponses);
    }
}

