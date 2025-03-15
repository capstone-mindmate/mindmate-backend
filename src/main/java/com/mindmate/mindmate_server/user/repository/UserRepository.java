package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String token);

//    boolean existsByNickname(String nickname);
}
