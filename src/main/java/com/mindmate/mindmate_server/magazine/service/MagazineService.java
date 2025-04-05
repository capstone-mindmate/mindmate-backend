package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.dto.MagazineCreateRequest;
import com.mindmate.mindmate_server.magazine.dto.MagazineDetailResponse;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.magazine.dto.MagazineUpdateRequest;

public interface MagazineService {

    MagazineResponse createMagazine(Long userId, MagazineCreateRequest request);

    MagazineResponse updateMagazine(Long magazineId, MagazineUpdateRequest request, Long userId);

    void deleteMagazine(Long magazineId, Long userId);

//    Page<MagazineResponse> getMagazines(Long userId, PageRequest createdAt);

    MagazineDetailResponse getMagazine(Long magazineId, Long userId);

    Magazine findMagazineById(Long magazineId);
}
