package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.MessageReactionResponse;
import com.mindmate.mindmate_server.chat.dto.ReactionRequest;
import com.mindmate.mindmate_server.chat.service.MessageReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "메시지 감정표현",
        description = "채팅 메시지에 대한 감정표현(이모지 리액션) 추가 및 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class MessageInteractionController {
    private final MessageReactionService reactionService;

    @Operation(
            summary = "메시지 감정표현 추가/변경/삭제",
            description = "채팅 메시지에 감정표현(이모지 리액션)을 추가, 변경 또는 삭제합니다. - websocket 장애시 사용 "
    )
    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<MessageReactionResponse> addReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId,
            @RequestBody @Valid ReactionRequest request) {
        // todo: dto 수정 or PathVariable 수정 -> 지금 중복 값임 + 텍스트일 때만 감정표현 가능하게?
        MessageReactionResponse response = reactionService.addReaction(principal.getUserId(), messageId, request.getReactionType());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "메시지 감정표현 목록 조회",
            description = "특정 메시지에 추가된 모든 감정표현(이모지 리액션) 목록을 조회합니다."
    )
    @GetMapping("/messages/{messageId}/reactions")
    public ResponseEntity<List<MessageReactionResponse>> getReactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long messageId) {
        List<MessageReactionResponse> reactionResponses = reactionService.getReactions(messageId);
        return ResponseEntity.ok(reactionResponses);
    }
}

