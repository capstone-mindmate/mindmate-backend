package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 가장 최근 메시지 조회
    Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Long roomId);

    // 채팅방의 메시지를 오래된 순으로 조회 (첫 방문용)
    Page<ChatMessage> findByChatRoomIdOrderByIdAsc(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    // 채팅방의 메시지를 최신순으로 조회 (재방문용)
    Page<ChatMessage> findByChatRoomIdOrderByIdDesc(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    // 특정 메시지 ID 이전의 메시지 조회 (스크롤 업용)
    Page<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(
            @Param("roomId") Long roomId,
            @Param("messageId") Long messageId,
            Pageable pageable
    );

    // 특정 메시지 ID 이상의 메시지 조회 (읽지 않은 메시지 포함)
    List<ChatMessage> findByChatRoomIdAndIdGreaterThanEqualOrderByIdAsc(
            @Param("roomId") Long roomId,
            @Param("messageId") Long messageId
    );

    // 특정 메시지 ID 이전의 N개 메시지 조회
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :roomId AND m.id <= :messageId ORDER BY m.id DESC")
    List<ChatMessage> findMessagesBeforeIdLimited(
            @Param("roomId") Long roomId,
            @Param("messageId") Long messageId,
            Pageable pageable
    );

    // 채팅방의 총 메시지 수 조회
    long countByChatRoomId(Long roomId);
}