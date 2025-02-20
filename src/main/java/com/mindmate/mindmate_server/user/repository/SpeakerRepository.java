package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpeakerRepository extends JpaRepository<SpeakerProfile, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    Optional<SpeakerProfile> findByUserId(Long userId);
}
