package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.dto.*;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MagazineService {

    MagazineResponse createMagazine(Long userId, MagazineCreateRequest request);

    MagazineResponse updateMagazine(Long magazineId, MagazineUpdateRequest request, Long userId);

    void deleteMagazine(Long magazineId, Long userId);

    Page<MagazineResponse> getMagazines(Long userId, MagazineSearchFilter filter, Pageable pageable);

    MagazineDetailResponse getMagazine(Long magazineId, Long userId);

    Magazine findMagazineById(Long magazineId);

    MagazineResponse manageMagazine(Long magazineId, boolean isAccepted);

    Page<MagazineResponse> getPendingMagazines(Pageable pageable);

    LikeResponse toggleLike(Long magazineId, Long userId);

    void handleEngagement(Long userId, Long magazineId, MagazineEngagementRequest request);

    List<MagazineResponse> getPopularMagazines(int limit);

    List<MagazineResponse> getPopularMagazinesByCategory(MatchingCategory category, int limit);

    Page<MagazineResponse> getMyMagazines(Long userId, Pageable pageable);

    Page<MagazineResponse> getLikedMagazines(Long userId, Pageable pageable);
}
