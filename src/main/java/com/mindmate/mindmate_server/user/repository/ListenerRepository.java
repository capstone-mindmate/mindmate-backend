package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.ListenerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListenerRepository extends JpaRepository<ListenerProfile, Long> {
    boolean existsByNickname(String nickname);
}
