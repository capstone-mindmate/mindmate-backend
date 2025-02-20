package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpeakerRepository extends JpaRepository<SpeakerProfile, Long> {
    boolean existsByNickname(String nickname);
}
