package com.mindmate.mindmate_server.emoticon.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonDetailResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonUploadRequest;
import com.mindmate.mindmate_server.emoticon.dto.UserEmoticonResponse;
import com.mindmate.mindmate_server.emoticon.service.EmoticonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(
        name = "이모티콘",
        description = "이모티콘 상점, 구매, 보유 이모티콘, 업로드 등 이모티콘 관련 API"
)
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/emoticons")
public class EmoticonController {
    private final EmoticonService emoticonService;

    @Operation(
            summary = "이모티콘 상점 목록 조회",
            description = "구매 가능한 모든 이모티콘 목록을 조회합니다."
    )
    @GetMapping("/shop")
    public ResponseEntity<List<EmoticonResponse>> getShopEmoticons(@AuthenticationPrincipal UserPrincipal principal) {
        List<EmoticonResponse> emoticons = emoticonService.getShopEmoticons(principal.getUserId());
        return ResponseEntity.ok(emoticons);
    }

    @Operation(
            summary = "이모티콘 상세 조회",
            description = "특정 이모티콘의 상세 정보(이미지, 가격, 유사 이모티콘 등)를 조회합니다."
    )
    @GetMapping("/detail/{emoticonId}")
    public ResponseEntity<EmoticonDetailResponse> getEmoticonDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emoticonId) {
        EmoticonDetailResponse emoticonDetail = emoticonService.getEmoticonDetail(emoticonId, principal.getUserId());
        return ResponseEntity.ok(emoticonDetail);
    }

    @Operation(
            summary = "내 보유 이모티콘 목록 조회",
            description = "사용자가 보유한 이모티콘과 아직 보유하지 않은 이모티콘 목록을 함께 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<UserEmoticonResponse> getMyEmoticons(@AuthenticationPrincipal UserPrincipal principal) {
        UserEmoticonResponse response = emoticonService.getUserEmoticons(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "채팅/매거진에서 사용 가능한 이모티콘 조회",
            description = "채팅방 또는 매거진에서 실제로 사용할 수 있는(보유 중인) 이모티콘 목록만 조회합니다."
    )
    @GetMapping("/available")
    public ResponseEntity<List<EmoticonResponse>> getAvailableEmoticons(@AuthenticationPrincipal UserPrincipal principal) {
        List<EmoticonResponse> emoticons = emoticonService.getAvailableEmoticons(principal.getUserId());
        return ResponseEntity.ok(emoticons);
    }


    @Operation(
            summary = "이모티콘 구매",
            description = "지정한 이모티콘을 포인트로 구매합니다."
    )
    @PostMapping("/purchase/{emoticonId}")
    public ResponseEntity<EmoticonResponse> purchaseEmoticon(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emoticonId) {
        EmoticonResponse response = emoticonService.purchaseEmoticon(principal.getUserId(), emoticonId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "이모티콘 등록(업로드)",
            description = "새로운 이모티콘을 파일 업로드 방식으로 등록합니다."
    )
    @PostMapping("/upload")
    public ResponseEntity<EmoticonResponse> uploadEmoticon(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") EmoticonUploadRequest request) {
        try {
            EmoticonResponse emoticon = emoticonService.uploadEmoticon(file, request, principal.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(emoticon);
        } catch (IOException e) {
            log.error("IOException during file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
