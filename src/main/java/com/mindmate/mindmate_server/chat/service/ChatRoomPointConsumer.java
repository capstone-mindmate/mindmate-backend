package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.dto.ChatRoomCloseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomPointConsumer {
    @KafkaListener(
            topics = "chat-room-close-topic",
            groupId = "close-point-group",
            containerFactory = "chatRoomCloseListenerContainerFactory"
    )
    @Transactional
    public void processChatRoomPoint(ConsumerRecord<String, ChatRoomCloseEvent> record) {
        // todo: 포인트 로직 완료 시 추가
    }
}
