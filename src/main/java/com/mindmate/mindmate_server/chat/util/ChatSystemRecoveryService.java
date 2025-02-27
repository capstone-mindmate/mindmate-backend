package com.mindmate.mindmate_server.chat.util;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.chat.dto.ChatRoomResponse;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
// todo : implements ApplicationListener<ApplicationReadyEvent>
public class ChatSystemRecoveryService  {
    private final ChatRoomService chatRoomService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyManager redisKeyManager;

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 애플리케이션 시작 시 Redis에 DB 값 동기화
     */
//    @Override
//    public void onApplicationEvent(ApplicationReadyEvent event) {
//        recoverUnreadCountsFromDb();
//    }

    /**
     * 자동 장애 복구
     */
    @Scheduled(fixedRate = 1800000)
    public void scheduledRecovery() {
        recoverUnreadCountsFromDb();
    }

    private void recoverUnreadCountsFromDb() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();
        for (ChatRoom room : chatRooms) {
            String listenerUnreadKey = redisKeyManager.getUnreadCountKey(
                    room.getId(),
                    // todo : LazyInitializationException 발생
                    room.getListener().getUser().getId()
            );
            redisTemplate.opsForValue().set(listenerUnreadKey, String.valueOf(room.getListenerUnreadCount()));

            String speakerUnreadKey = redisKeyManager.getUnreadCountKey(
                    room.getId(),
                    room.getSpeaker().getUser().getId()
            );
            redisTemplate.opsForValue().set(speakerUnreadKey, String.valueOf(room.getSpeakerUnreadCount()));
        }
    }
}
