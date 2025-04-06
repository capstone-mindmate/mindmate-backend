package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.magazine.dto.MagazineCategoryStatistics;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MagazineStatisticsService {
    private final MagazineService magazineService;
    private final MagazineRepository magazineRepository;

    public List<MagazineCategoryStatistics> getCategoryStatistics() {
        return magazineRepository.getCategoryStatistics();
    }
}
