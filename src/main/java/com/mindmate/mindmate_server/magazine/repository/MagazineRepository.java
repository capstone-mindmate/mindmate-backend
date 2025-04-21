package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.domain.MagazineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MagazineRepository extends JpaRepository<Magazine, Long>, MagazineRepositoryCustom {
    Page<Magazine> findByMagazineStatus(MagazineStatus status, Pageable pageable);
}
