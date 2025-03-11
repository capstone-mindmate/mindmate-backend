package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//@Slf4j
//@Service
//@Transactional
//@RequiredArgsConstructor
//public class MessageStorageConsumer {
    /**
     * 1. 메시지 저장
     * 2. 메시지 필터링
     * 3. 읽은 안읽음 처리
     * 4. 알림 처리
     */
//    private final ChatMessageRepository chatMessageRepository;
//
//    private final UserService userService;
//    private final ChatRoomService chatRoomService;



//    @KafkaListener(
//            topics = "chat-message-topic",
//            groupId = "message-storage-group",
//            containerFactory = "chatMessageListenerContainerFactory"
//    )
//    public void processMessageStorage(ConsumerRecord<String, ChatMessageEvent> record) {
//        ChatMessageEvent event = record.value();
//        log.info("Consumed chat message for storage: {}", event);
//
//        try {
//            ChatRoom chatRoom = chatRoomService.findChatRoomById(event.getRoomId());
//            User sender = userService.findUserById(event.getSenderId());
//
//            ChatMessage chatMessage = ChatMessage.builder()
//                    .chatRoom(chatRoom)
//                    .sender(sender)
//                    .senderRole(event.getSenderRole())
//                    .content(event.getContent())
//                    .type(event.getType())
//                    .build();
//
//            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
//
//            event.setId(savedMessage.getId());
//
//            chatRoom.updateLastMessageTime();
////            chatRoomRepository.save(chatRoom);
//            log.info("Message saved successfully: id={}", savedMessage.getId());
//        } catch (Exception e) {
//            log.error("Error saving message", e);
//        }
//    }
//}
