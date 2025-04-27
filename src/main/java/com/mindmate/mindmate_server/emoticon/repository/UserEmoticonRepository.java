package com.mindmate.mindmate_server.emoticon.repository;

import com.mindmate.mindmate_server.emoticon.domain.UserEmoticon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserEmoticonRepository extends JpaRepository<UserEmoticon, Long> {
    List<UserEmoticon> findByUserId(Long userId);

    Optional<UserEmoticon> findByUserIdAndEmoticonId(Long userId, Long emoticonId);

    boolean existsByUserIdAndEmoticonId(Long userId, Long emoticonId);
}
