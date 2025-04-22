package com.mindmate.mindmate_server.emoticon.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonDetailResponse;
import com.mindmate.mindmate_server.emoticon.dto.EmoticonResponse;
import com.mindmate.mindmate_server.emoticon.dto.UserEmoticonResponse;
import com.mindmate.mindmate_server.emoticon.service.EmoticonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emoticons")
public class EmoticonController {
    private final EmoticonService emoticonService;
    /**
     * 이모티콘 샾에서 조회
     * -> 구매 가능한 이모티콘
     */
    @GetMapping("/shop")
    public ResponseEntity<List<EmoticonResponse>> getShopEmoticons(@AuthenticationPrincipal UserPrincipal principal) {
        List<EmoticonResponse> emoticons = emoticonService.getShopEmoticons(principal.getUserId());
        return ResponseEntity.ok(emoticons);
    }

    /**
     * 이모티콘 상세 조회
     * -> 샾에서 해당 이모티콘 클릭했을 때 이미지, 가격, 다른 구매 가능한 이모티콘 보여주기
     */
    @GetMapping("/{emoticon}")
    public ResponseEntity<EmoticonDetailResponse> getEmoticonDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emoticonId) {
        EmoticonDetailResponse emoticonDetail = emoticonService.getEmoticonDetail(emoticonId, principal.getUserId());
        return ResponseEntity.ok(emoticonDetail);
    }

    /**
     * 자신의 보유 이모티콘 확인 + 보유하지 않은 다른 이모티콘도 보여주기
     */
    @GetMapping("/my")
    public ResponseEntity<UserEmoticonResponse> getMyEmoticons(@AuthenticationPrincipal UserPrincipal principal) {
        UserEmoticonResponse response = emoticonService.getUserEmoticons(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅 or 매거진에서 이모티콘 클릭 시 보여주는 조회 기능 -> 보유한 것만
     */
    @GetMapping("/available")
    public ResponseEntity<List<EmoticonResponse>> getAvailableEmoticons(@AuthenticationPrincipal UserPrincipal principal) {
        List<EmoticonResponse> emoticons = emoticonService.getAvailableEmoticons(principal.getUserId());
        return ResponseEntity.ok(emoticons);
    }



    /**
     * 이모티콘 구매하기
     */
}
