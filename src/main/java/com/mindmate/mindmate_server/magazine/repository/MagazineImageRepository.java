package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MagazineImageRepository extends JpaRepository<MagazineImage, Long> {

//    List<MagazineImage> findByMagazineIsNullAndCreatedAtBefore(LocalDateTime localDateTime);

    List<MagazineImage> findByCreatedAtBefore(LocalDateTime localDateTime);
}
