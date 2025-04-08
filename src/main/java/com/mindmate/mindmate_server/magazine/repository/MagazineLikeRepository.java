package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineLike;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MagazineLikeRepository extends JpaRepository<MagazineLike, Long> {

    boolean existsByMagazineAndUser(Magazine magazine, User user);

    void deleteByMagazineAndUser(Magazine magazine, User user);
}
