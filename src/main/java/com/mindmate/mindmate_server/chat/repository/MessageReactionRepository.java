package com.mindmate.mindmate_server.chat.repository;

import com.mindmate.mindmate_server.chat.domain.MessageReaction;
import com.mindmate.mindmate_server.chat.domain.ReactionType;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);
    Optional<MessageReaction> findByMessageIdAndUserIdAndReactionType(Long messageId, Long userId, ReactionType reactionType);
    List<MessageReaction> findAllByMessageId(Long messageId);

    @Query("SELECT mr.reactionType, COUNT(mr) FROM MessageReaction mr WHERE mr.message.id = :messageId GROUP BY mr.reactionType")
    List<Object[]> countReactionsByMessageId(@Param("messageId") Long messageId);
}
