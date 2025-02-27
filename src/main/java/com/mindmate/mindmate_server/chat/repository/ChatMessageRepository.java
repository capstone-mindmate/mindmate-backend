package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Long roomId);

    List<ChatMessage> findByChatRoomIdAndIdGreaterThan(
            @Param("roomId") Long roomId,
            @Param("lastReadId") Long lastReadId
    );
}
