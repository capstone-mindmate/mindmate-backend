package com.mindmate.mindmate_server.magazine.repository;

import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.dto.MagazineSearchFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagazineRepositoryCustom {
    Page<MagazineResponse> findMagazinesWithFilters(MagazineSearchFilter filter, Pageable pageable);
}
