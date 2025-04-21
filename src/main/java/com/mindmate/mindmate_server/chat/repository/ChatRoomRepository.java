package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {
//    @Query("SELECT cr FROM ChatRoom cr WHERE cr.listener.id = :user OR cr.speaker.id = :user")
//    Page<ChatRoom> findAllByParticipant(@Param("user") Long userID, Pageable pageable);

//    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.listener.id = :userId AND cr.listener.currentRole = :roleType) OR (cr.speaker.id = :userId AND cr.speaker.currentRole = :roleType)")
//    Page<ChatRoom> findAllByParticipantAndRole(@Param("userId") Long userId, @Param("roleType") String roleType, Pageable pageable);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.matching m " +
            "WHERE m.creator.id = :userId OR m.acceptedUser.id = :userId")
    Page<ChatRoom> findAllByParticipant(@Param("userId") Long userId, Pageable pageable);

    // todo: 매칭방 엔티티 설계에 따라 변경
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.matching m " +
            "WHERE (m.creatorRole  = 'SPEAKER' AND m.creator.id = :userID) OR " +
            "(m.creatorRole  = 'LISTENER' AND m.acceptedUser.id = :userId)")
    Page<ChatRoom> findAllyBySpeaker(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.matching m " +
            "WHERE (m.creatorRole  = 'LISTENER' AND m.creator.id = :userId) OR " +
            "(m.creatorRole = 'SPEAKER' AND m.acceptedUser.id = :userId)")
    Page<ChatRoom> findAllByListener(@Param("userId") Long userId, Pageable pageable);
}
