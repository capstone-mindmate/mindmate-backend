package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.listener.id = :user OR cr.speaker.id = :user")
    Page<ChatRoom> findAllByParticipant(@Param("user") Long userID, Pageable pageable);

    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.listener.id = :userId AND cr.listener.currentRole = :roleType) OR (cr.speaker.id = :userId AND cr.speaker.currentRole = :roleType)")
    Page<ChatRoom> findAllByParticipantAndRole(@Param("userId") Long userId, @Param("roleType") String roleType, Pageable pageable);
}
