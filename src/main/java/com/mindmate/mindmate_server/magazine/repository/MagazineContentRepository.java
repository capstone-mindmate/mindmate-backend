package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineContent;
import com.mindmate.mindmate_server.magazine.domain.MagazineContentType;
import com.mindmate.mindmate_server.magazine.domain.MagazineImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MagazineContentRepository extends JpaRepository<MagazineContent, Long> {
//    List<MagazineContent> findByMagazineOrderByContentOrderAsc(Magazine magazine);

    void deleteByMagazine(Magazine magazine);

    Optional<MagazineContent> findByImageAndType(MagazineImage image, MagazineContentType type);

    boolean existsByImage(MagazineImage image);

    List<MagazineContent> findByImage(MagazineImage magazineImage);
}
