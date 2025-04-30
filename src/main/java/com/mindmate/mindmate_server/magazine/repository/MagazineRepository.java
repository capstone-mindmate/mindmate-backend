package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MagazineRepository extends JpaRepository<Magazine, Long>, MagazineRepositoryCustom {
    Page<Magazine> findByMagazineStatus(MagazineStatus status, Pageable pageable);

    List<Magazine> findTop10ByMagazineStatusOrderByLikeCountDesc(MagazineStatus status);

    List<Magazine> findByMagazineStatusAndCategoryOrderByLikeCountDesc(MagazineStatus status, MatchingCategory category);
}
