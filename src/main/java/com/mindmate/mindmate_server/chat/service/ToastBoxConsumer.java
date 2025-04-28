package com.mindmate.mindmate_server.chat.service;

import com.mindmate.mindmate_server.chat.domain.MessageType;
import com.mindmate.mindmate_server.chat.domain.ToastBoxKeyword;
import com.mindmate.mindmate_server.chat.dto.ChatEventType;
import com.mindmate.mindmate_server.chat.dto.ChatMessageEvent;
import com.mindmate.mindmate_server.chat.dto.ToastBoxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToastBoxConsumer {
    private final ToastBoxService toastBoxService;
    private final ChatEventPublisher eventPublisher;

    // todo: 지금 근데 이게 따로 메시지의 형태로 저장되는 게 아님 -> 실시간 채팅 중에만 확인 가능
    @KafkaListener(
            topics = "chat-message-topic",
            groupId = "toast-box-group",
            containerFactory = "chatMessageListenerContainerFactory"
    )
    public void processToastBox(ConsumerRecord<String, ChatMessageEvent> record) {
        ChatMessageEvent event = record.value();

        if (event.isFiltered() || event.getMessageId() == null || event.getType() == MessageType.EMOTICON) {
            return;
        }

        try {
            List<ToastBoxKeyword> keywords = toastBoxService.findToastBoxKeywords(event.getContent());

            if (!keywords.isEmpty()) {
                for (ToastBoxKeyword keyword : keywords) {
                    ToastBoxEvent toastBoxEvent = ToastBoxEvent.builder()
                            .roomId(event.getRoomId())
                            .messageId(event.getMessageId())
                            .keyword(keyword.getKeyword())
                            .title(keyword.getTitle())
                            .content(keyword.getContent())
                            .linkUrl(keyword.getLinkUrl())
                            .imageUrl(keyword.getImageUrl())
                            .build();

                    eventPublisher.publishChatRoomEvent(
                            event.getRoomId(),
                            ChatEventType.TOAST_BOX,
                            toastBoxEvent
                    );

                    log.info("토스트 박스 이벤트 발행: 방={}, 키워드={}",
                            event.getRoomId(), keyword.getKeyword());
                }
            }
        } catch (Exception e) {
            log.error("토스트 박스 처리 중 오류: {}", e.getMessage(), e);
        }
    }
}
