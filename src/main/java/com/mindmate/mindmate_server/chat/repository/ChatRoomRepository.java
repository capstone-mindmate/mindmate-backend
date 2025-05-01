package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.chatRoomStatus = 'ACTIVE' AND " +
            "(cr.matching.creator.id = :userId OR cr.matching.acceptedUser.id = :userId)")
    List<ChatRoom> findActiveChatRoomByUserId(Long userId);
}
