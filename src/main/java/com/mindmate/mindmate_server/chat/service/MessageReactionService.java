package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.chat.domain.ReactionType;
import com.mindmate.mindmate_server.chat.dto.MessageReactionResponse;
import com.mindmate.mindmate_server.chat.repository.MessageReactionRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageReactionService {
    private final MessageReactionRepository messageReactionRepository;
    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;

    @Transactional
    public MessageReactionResponse addReaction(Long userId, Long messageId, ReactionType reactionType) {
        User user = userService.findUserById(userId);
        ChatMessage chatMessage = chatMessageService.findChatMessageById(messageId);

        chatRoomService.validateChatActivity(userId, chatMessage.getChatRoom().getId());

        Optional<MessageReaction> existingReaction = messageReactionRepository
                .findByMessageIdAndUserId(messageId, userId);


        // 1. 이미 해당 채팅에 리액션 남긴 경우
        if (existingReaction.isPresent()) {
            MessageReaction reaction = existingReaction.get();

            // 1-1. 같은 타입일 경우 -> 삭제
            if (reaction.getReactionType() == reactionType) {
                messageReactionRepository.delete(reaction);
                return MessageReactionResponse.builder()
                        .id(null)
                        .messageId(messageId)
                        .userId(userId)
                        .userName(user.getProfile().getNickname())
                        .reactionType(reactionType)
//                        .removed(true)
                        .build();
                // 1-2. 다른 타입일 경우 -> 업데이트
            } else {
                reaction.updateReactionType(reactionType);
                // 굳이?
                MessageReaction savedReaction = messageReactionRepository.save(reaction);
                return MessageReactionResponse.from(savedReaction);
            }
            // 2. 기존 리액션 없는 경우 -> 새로 생성
        } else {
            MessageReaction reaction = MessageReaction.builder()
                    .message(chatMessage)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            MessageReaction savedReaction = messageReactionRepository.save(reaction);
            return MessageReactionResponse.from(savedReaction);
        }
    }

//    @Transactional
//    public void removeReaction(Long userId, Long messageId, ReactionType reactionType) {
////        User user = userService.findUserById(userId);
//        ChatMessage chatMessage = chatMessageService.findChatMessageById(messageId);
//        ChatRoom chatRoom = chatMessage.getChatRoom();
//
//        chatRoomService.validateChatActivity(userId, chatRoom.getId());
//
//        messageReactionRepository.findByMessageIdAndUserIdAndReactionType(messageId, userId, reactionType)
//                .ifPresent(messageReactionRepository::delete);
//    }

    @Transactional(readOnly = true)
    public List<MessageReactionResponse> getReactions(Long messageId) {
        return messageReactionRepository.findAllByMessageId(messageId).stream()
                .map(MessageReactionResponse::from)
                .collect(Collectors.toList());
    }
}
