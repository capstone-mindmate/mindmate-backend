package com.mindmate.mindmate_server.magazine.service;

import com.mindmate.mindmate_server.magazine.dto.MagazineCategoryStatistics;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MagazineStatisticsServiceTest {
    @Mock private MagazineService magazineService;
    @Mock private MagazineRepository magazineRepository;

    @InjectMocks
    private MagazineStatisticsService magazineStatisticsService;

    @Test
    @DisplayName("카테고리별 통계 조회 성공 테스트")
    void getCategoryStatistics_Success() {
        // given
        List<MagazineCategoryStatistics> mockStatistics = Arrays.asList(
                new MagazineCategoryStatistics(MatchingCategory.CAREER, 10, 50),
                new MagazineCategoryStatistics(MatchingCategory.ACADEMIC, 5, 25),
                new MagazineCategoryStatistics(MatchingCategory.RELATIONSHIP, 3, 15)
        );

        when(magazineRepository.getCategoryStatistics()).thenReturn(mockStatistics);

        // when
        List<MagazineCategoryStatistics> result = magazineStatisticsService.getCategoryStatistics();

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(MatchingCategory.CAREER, result.get(0).getCategory());
        assertEquals(10, result.get(0).getMagazineCount());
        assertEquals(50, result.get(0).getTotalLikes());
        verify(magazineRepository).getCategoryStatistics();
    }


}