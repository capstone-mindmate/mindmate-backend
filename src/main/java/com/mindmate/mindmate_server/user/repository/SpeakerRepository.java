package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import com.mindmate.mindmate_server.user.domain.SpeakerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeakerRepository extends JpaRepository<SpeakerProfile, Long> {
    boolean existsByNickname(String nickname);

    // 상담 스타일
    List<SpeakerProfile> findByPreferredCounselingStyle(CounselingStyle preferredCounselingStyle);
}
