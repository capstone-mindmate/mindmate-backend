package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.magazine.domain.Magazine;
import com.mindmate.mindmate_server.magazine.dto.MagazineResponse;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;

import java.util.List;

public interface MagazinePopularityService {
    void incrementViewCount(Magazine magazine, Long userId, String ipAddress);

    void initializePopularityScore(Magazine magazine);

    void updateLikeScore(Magazine magazine, boolean isLiked);

    void processEngagement(Long magazineId, Long userId, Long dwellTime, Double scrollPercentage);

    void removePopularityScores(Long magazineId, MatchingCategory category);

    List<MagazineResponse> getPopularMagazines(int limit);

    List<MagazineResponse> getPopularMagazinesByCategory(String category, int limit);
}
