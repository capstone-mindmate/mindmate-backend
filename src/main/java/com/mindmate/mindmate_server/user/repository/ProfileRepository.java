package com.mindmate.mindmate_server.user.repository;

import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    Optional<Profile> findByUser(User user);

    boolean existsByNickname(String nickname);

    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.user.id = :userId")
    Optional<Profile> findWithUserByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Profile p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    Page<Profile> findAllWithUser(Pageable pageable);

}
