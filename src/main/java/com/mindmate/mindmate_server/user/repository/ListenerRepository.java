package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ListenerRepository extends JpaRepository<ListenerProfile, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    Optional<ListenerProfile> findByUserId(Long userId);
}
