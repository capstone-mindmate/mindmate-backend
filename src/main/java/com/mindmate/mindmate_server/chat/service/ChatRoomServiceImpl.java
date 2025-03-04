package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomDetailResponse;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.global.exception.ChatErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

//    private final UserService userService;

    @Override
    public ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsForUser(Long userId, PageRequest pageRequest) {
        return chatRoomRepository.findAllByParticipant(userId, pageRequest)
                .map(chatRoom -> ChatRoomResponse.from(chatRoom, userId));
    }

    @Override
    public Page<ChatRoomResponse> getChatRoomsByUserRole(Long userId, PageRequest pageRequest, RoleType roleType) {
        return chatRoomRepository.findAllByParticipantAndRole(userId, roleType.getKey(), pageRequest)
                .map(chatRoom -> ChatRoomResponse.from(chatRoom, userId));
    }

    @Override
    public ChatRoomDetailResponse getInitialMessages(Long userId, Long roomId, int size) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        boolean isListener = chatRoom.getListener().getUser().getId().equals(userId);

        Long lastReadMessageId = isListener
                ? chatRoom.getListenerLastReadMessageId()
                : chatRoom.getSpeakerLastReadMessageId();

        List<ChatMessage> messages;

        // 채팅방 총 메시지 수 확인
        long totalMessages = chatMessageRepository.countByChatRoomId(roomId);

        if (lastReadMessageId == 0) {
            // 1. 처음 채팅방에 들어가는 경우 -> 가장 오래된 메시지부터 보여주기
            messages = new ArrayList<>(chatMessageRepository.findByChatRoomIdOrderByIdAsc(
                    roomId, PageRequest.of(0, (int)Math.min(totalMessages, size))).getContent());
        } else {
            // 마지막으로 읽은 메시지 이후의 메시지가 있는지 확인
            Optional<ChatMessage> latestMessageOpt = chatMessageRepository.findTopByChatRoomIdOrderByIdDesc(roomId);

            if (latestMessageOpt.isPresent() && latestMessageOpt.get().getId() > lastReadMessageId) {
                // 2. 읽지 않은 메시지가 있는 경우
                // 마지막으로 읽은 메시지 이전의 10개 메시지 가져오기
                List<ChatMessage> previousMessages = new ArrayList<>(chatMessageRepository.findMessagesBeforeIdLimited(
                        roomId, lastReadMessageId, PageRequest.of(0, 10)));

                // 마지막으로 읽은 메시지 포함 이후의 모든 새 메시지 가져오기
                List<ChatMessage> newMessages = chatMessageRepository.findByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(
                        roomId, lastReadMessageId);

                // 이전 메시지는 역순이므로 뒤집어서 시간순 정렬
                Collections.reverse(previousMessages);

                // 두 리스트 합치기
                messages = new ArrayList<>();
                messages.addAll(previousMessages);
                messages.addAll(newMessages);
            } else {
                // 3. 안 읽은 메시지가 없을 때 -> 최신 메시지 보여주기
                List<ChatMessage> tempMessages = chatMessageRepository.findByChatRoomIdOrderByIdDesc(
                        roomId, PageRequest.of(0, size)).getContent();
                messages = new ArrayList<>(tempMessages);
                Collections.reverse(messages); // 시간순 정렬
            }
        }

        return ChatRoomDetailResponse.from(chatRoom, messages);
    }

    @Override
    public List<ChatMessageResponse> getPreviousMessages(Long roomId, Long messageId, int size) {
        // 4. 이전 메시지 페이지네이션 (스크롤 업)
        List<ChatMessage> tempMessages = chatMessageRepository.findByChatRoomIdAndIdLessThanOrderByIdDesc(
                roomId, messageId, PageRequest.of(0, size)).getContent();

        // 시간순 정렬 (오래된 메시지부터)
        List<ChatMessage> messages = new ArrayList<>(tempMessages);
        Collections.reverse(messages);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void closeChatRoom(Long userId, Long roomId) {
        ChatRoom chatRoom = findChatRoomById(roomId);
        if (!chatRoom.getListener().getUser().getId().equals(userId) &&
                !chatRoom.getSpeaker().getUser().getId().equals(userId)) {
            throw new CustomException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        chatRoom.close();
        log.info("Closed chat room {}", roomId);
    }
}
