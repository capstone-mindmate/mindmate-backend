package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.ChatMessageRequest;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.chat.service.ChatPresenceService;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
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

    /**
     * 사용자의 모든 채팅방 목록 조회
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
     */
    @GetMapping("/rooms/role/{roleType}")
    public ResponseEntity<Page<ChatRoomResponse>> getChatRoomsByRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable RoleType roleType) {
        Page<ChatRoomResponse> rooms = chatRoomService.getChatRoomsByUserRole(
                principal.getUserId(),
                PageRequest.of(page, size, Sort.by("lastMessageTime").descending()),
                roleType);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 채팅방 입장 시 메시지 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatRoomDetailResponse> getChatRoomWithMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "20") int size) {

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
            @RequestParam(defaultValue = "20") int size) {

        List<ChatMessageResponse> messages = chatRoomService.getPreviousMessages(
                roomId, messageId, size);

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
     * 채팅방 종료
     * todo : 매칭 종료와 어떤 식으로 분리해서 처리해야 할지 고민
     */
    @PostMapping("/rooms/{roomId}/close")
    public ResponseEntity<Void> closeChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.closeChatRoom(principal.getUserId(), roomId);

        // 채팅방 상태 변경 알림
//        ChatRoomStatusNotification notification = ChatRoomStatusNotification.builder()
//                .roomId(roomId)
//                .status("CLOSED")
//                .updatedBy(principal.getUserId())
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        messagingTemplate.convertAndSend("/topic/chat.room." + roomId + ".status", notification);


        return ResponseEntity.ok().build();
    }
}
