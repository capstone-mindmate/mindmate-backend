package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.service.ChatPresenceService;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.chat.service.ChatSearchService;
import com.mindmate.mindmate_server.chat.service.ChatService;
import com.mindmate.mindmate_server.user.domain.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final ChatPresenceService chatPresenceService;
    private final ChatRoomService chatRoomService;
    private final ChatSearchService chatSearchService;

    /**
     * 사용자의 모든 채팅방 목록 조회
     * todo: 채팅방 상태에 따른 목록 조회를 따로 api 뺼건지, 프론트에서 어차피 값 있으니까 렌더링만 바꿔서 보여줄지
     */
    @GetMapping("/rooms")
    public ResponseEntity<Page<ChatRoomResponse>> getChatRooms(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ChatRoomResponse> rooms = chatRoomService.getChatRoomsForUser(
                principal.getUserId(),
                PageRequest.of(page, size, Sort.by("lastMessageTime").descending())
        );
        return ResponseEntity.ok(rooms);
    }


    /**
     * 특정 역할로 참여 중인 채팅방 목록 조회
     * roleType: LISTENER, SPEAKER
     */
    @GetMapping("/rooms/role/{roleType}")
    public ResponseEntity<Page<ChatRoomResponse>> getChatRoomsByRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable String roleType) {
        Page<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByUserRole(
                principal.getUserId(),
                PageRequest.of(page, size, Sort.by("lastMessageTime").descending()), roleType);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 특정 상태의 채팅방 목록 조회
     * status: ACTIVE, CLOSED 등
     */
    @GetMapping("/rooms/status/{status}")
    public ResponseEntity<Page<ChatRoomResponse>> getChatRoomsByStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable ChatRoomStatus status) {
        Page<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByUserAndStatus(
                principal.getUserId(),
                PageRequest.of(page, size, Sort.by("lastMessageTime").descending()),
                status);

        return ResponseEntity.ok(rooms);
    }

    /**
     * 채팅방 입장 시 메시지 조회
     * todo: custom-form 답변 시 system 답변 필요하지 않나?
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatRoomDetailResponse> getChatRoomWithMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "30") int size) {

        ChatRoomDetailResponse response = chatRoomService.getInitialMessages(
                principal.getUserId(), roomId, size);

        chatPresenceService.updateUserStatus(principal.getUserId(), true, roomId);
        chatService.markAsRead(principal.getUserId(), roomId);

        return ResponseEntity.ok(response);
    }

    /**
     * 이전 메시지 로드
     */
    @GetMapping("/rooms/{roomId}/messages/before/{messageId}")
    public ResponseEntity<List<ChatMessageResponse>> getPreviousMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestParam(defaultValue = "30") int size) {

        List<ChatMessageResponse> messages = chatRoomService.getPreviousMessages(
                roomId, messageId, principal.getUserId(), size);

        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 전송 - rest api 버전
     * websocket 연결이 불안정/불가능한 상황 사용 (동일 로직임)
     */
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ChatMessageRequest request) {
        ChatMessageResponse response = chatService.sendMessage(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 채팅방 내 메시지 검색
     */
    @GetMapping("/rooms/{roomId}/search")
    public ResponseEntity<ChatSearchResponse> searchMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam String keyword,
            @RequestParam(required = false) Long oldestLoadedMessageId,
            @RequestParam(required = false) Long newestLoadedMessageId) {
        ChatSearchResponse response = chatSearchService.searchMessages(principal.getUserId(), roomId, keyword, oldestLoadedMessageId, newestLoadedMessageId);
        return ResponseEntity.ok(response);
    }

    /**
     * 검색 결과 네비게이션
     */
    @GetMapping("/rooms/{roomId}/search/navigate")
    public ResponseEntity<SearchNavigationResponse> navigateToSearchResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam String keyword,
            @RequestParam Long targetMessageId,
            @RequestParam Long oldestLoadedMessageId) {
        SearchNavigationResponse response = chatSearchService.navigateToSearchResult(
                principal.getUserId(), roomId, keyword, targetMessageId, oldestLoadedMessageId);
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 종료 요청
     */
    @PostMapping("/rooms/{roomId}/close")
    public ResponseEntity<Void> closeChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.closeChatRoom(principal.getUserId(), roomId);

        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방 종료 수락
     * 매칭방 상태까지 바꿔줘야 함
     */
    @PostMapping("/rooms/{roomId}/close/accept")
    public ResponseEntity<Void> acceptCloseChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.acceptCloseChatRoom(principal.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방 종료 거절
     */
    @PostMapping("/rooms/{roomId}/close/reject")
    public ResponseEntity<Void> rejectCloseChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.rejectCloseChatRoom(principal.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }
}
