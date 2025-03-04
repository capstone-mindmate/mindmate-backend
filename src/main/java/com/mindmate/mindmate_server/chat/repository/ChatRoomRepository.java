package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import com.mindmate.mindmate_server.user.domain.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.listener.user.id = :user OR cr.speaker.user.id = :user")
    Page<ChatRoom> findAllByParticipant(@Param("user") Long userID, Pageable pageable);

    @Query("SELECT cr FROM ChatRoom cr WHERE " +
            "(cr.listener.user.id = :user AND :roleTypeKey = 'ROLE_LISTENER') OR " +
            "(cr.speaker.user.id = :user AND :roleTypeKey = 'ROLE_SPEAKER')")
    Page<ChatRoom> findAllByParticipantAndRole(@Param("user") Long userId,
                                               @Param("roleTypeKey") String roleTypeKey,
                                               Pageable pageable);


    Optional<ChatRoom> findByMatchingId(Long matchingId);
}
