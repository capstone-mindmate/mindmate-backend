package com.mindmate.mindmate_server.chat.controller;

import com.mindmate.mindmate_server.chat.domain.ChatRoomStatus;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.chat.dto.*;
import com.mindmate.mindmate_server.chat.service.ChatPresenceService;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.chat.service.ChatSearchService;
import com.mindmate.mindmate_server.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "채팅",
        description = "채팅방 목록 조회, 검색, 읽음/안읽음 등 채팅 관련 API"
)
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final ChatPresenceService chatPresenceService;
    private final ChatRoomService chatRoomService;
    private final ChatSearchService chatSearchService;

    @Operation(
            summary = "내 모든 채팅방 목록 조회",
            description = "사용자가 참여 중인 모든 채팅방 목록을 최신 메시지 순으로 정렬하여 조회합니다."
    )
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


    @Operation(
            summary = "역할별 채팅방 목록 조회",
            description = "특정 역할(LISTENER, SPEAKER)로 참여 중인 채팅방 목록을 조회합니다."
    )
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

    @Operation(
            summary = "상태별 채팅방 목록 조회",
            description = "특정 상태의 채팅방 목록을 조회합니다." +
                    "ACTIVE         대화 중\n" +
                    "PENDING        매칭 잡는 중\n" +
                    "CLOSED         대화 끝\n" +
                    "CLOSE_REQUEST  종료 요청"
    )
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

    @Operation(
            summary = "채팅방 입장 시 메시지 조회",
            description = "채팅방에 입장할 때 최근 메시지 목록과 방 정보를 함께 조회합니다."
    )
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

    @Operation(
            summary = "이전 메시지 로드",
            description = "특정 메시지 이전의 과거 메시지들을 조회합니다. 무한 스크롤 등에서 사용."
    )
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

    @Operation(
            summary = "메시지 전송 (REST)",
            description = "REST API 방식으로 채팅 메시지를 전송합니다. WebSocket이 불안정할 때 사용"
    )
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ChatMessageRequest request) {
        ChatMessageResponse response = chatService.sendMessage(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "채팅방 내 메시지 검색",
            description = """
                채팅방 내에서 키워드로 메시지를 검색합니다.
            
                [파라미터]
                - oldestLoadedMessageId (optional): 프론트엔드에서 현재 화면에 로드된 메시지 중 가장 오래된(위쪽) 메시지의 ID
                - newestLoadedMessageId (optional): 프론트엔드에서 현재 화면에 로드된 메시지 중 가장 최근(아래쪽) 메시지의 ID
            
                [동작]
                - 서버는 해당 채팅방에서 키워드에 매칭되는 모든 메시지의 ID 목록을 반환합니다.
                - oldestLoadedMessageId와 newestLoadedMessageId를 함께 전달하면,
                  현재 화면에 이미 보이는 메시지 중 검색 결과에 포함된 첫 번째 메시지의 ID(firstVisibleMatchId)도 함께 반환합니다.
                - 프론트엔드는 이 값을 이용해 검색 결과 중 화면에 이미 보이는 메시지를 하이라이트할 수 있습니다.
            """
    )
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

    @Operation(
            summary = "검색 결과 네비게이션",
            description = """
                검색 결과 중 특정 메시지로 이동할 때 추가 메시지 목록을 조회합니다.
            
                [파라미터]
                - targetMessageId: 사용자가 이동하고자 하는 검색 결과 메시지의 ID
                - oldestLoadedMessageId: 프론트엔드에서 현재 화면에 로드된 메시지 중 가장 오래된(위쪽) 메시지의 ID
            
                [동작]
                - targetMessageId가 현재 화면에 없는 과거 메시지라면,
                  서버는 targetMessageId ~ oldestLoadedMessageId-1 범위의 메시지 목록을 추가로 반환합니다.
                - 프론트엔드는 이 메시지들을 화면에 추가로 표시하고, targetMessageId 위치로 스크롤 이동할 수 있습니다.
                - 검색 결과의 현재 인덱스, 전체 매칭 개수 등도 함께 반환합니다.
            """
    )
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

    @Operation(
            summary = "채팅방 종료 요청",
            description = "채팅방 종료를 요청합니다"
    )
    @PostMapping("/rooms/{roomId}/close")
    public ResponseEntity<Void> closeChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.closeChatRoom(principal.getUserId(), roomId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "채팅방 종료 수락",
            description = "상대방의 채팅방 종료 요청을 수락합니다."
    )
    @PostMapping("/rooms/{roomId}/close/accept")
    public ResponseEntity<Void> acceptCloseChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.acceptCloseChatRoom(principal.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "채팅방 종료 거절",
            description = "상대방의 채팅방 종료 요청을 거절합니다."
    )
    @PostMapping("/rooms/{roomId}/close/reject")
    public ResponseEntity<Void> rejectCloseChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.rejectCloseChatRoom(principal.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "내 전체 읽지 않은 메시지 수 조회",
            description = "사용자의 모든 채팅방에서 읽지 않은 메시지의 총 개수를 조회합니다. 처음 로그인, 서비스 접속시 사용하고 후에는 websocket으로 연결"
    )
    @GetMapping("/unread/total")
    public ResponseEntity<Long> getTotalUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();
        Long totalCount = chatPresenceService.getTotalUnreadCount(userId);
        return ResponseEntity.ok(totalCount);
    }

    @Operation(
            summary = "채팅방 삭제",
            description = "해당 채팅방에 대해 삭제 상태로 변경합니다"
    )
    @PostMapping("/rooms/{roomId}/delete")
    public ResponseEntity<Void> deleteChatRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        chatRoomService.deleteChatRoomForUser(principal.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }
}
