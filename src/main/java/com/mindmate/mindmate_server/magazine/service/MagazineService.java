package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagazineService {

    MagazineResponse createMagazine(Long userId, MagazineCreateRequest request);

    MagazineResponse updateMagazine(Long magazineId, MagazineUpdateRequest request, Long userId);

    void deleteMagazine(Long magazineId, Long userId);

    Page<MagazineResponse> getMagazines(Long userId, MagazineSearchFilter filter, Pageable pageable);

    MagazineDetailResponse getMagazine(Long magazineId, Long userId);

    Magazine findMagazineById(Long magazineId);

    MagazineResponse publishMagazine(Long magazineId);

    MagazineResponse rejectMagazine(Long magazineId);

    Page<MagazineResponse> getPendingMagazines(Pageable pageable);
}
