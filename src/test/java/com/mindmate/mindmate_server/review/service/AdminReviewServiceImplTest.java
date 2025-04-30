package com.mindmate.mindmate_server.review.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReviewErrorCode;
import com.mindmate.mindmate_server.report.service.ReportService;
import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import com.mindmate.mindmate_server.review.domain.Review;
import com.mindmate.mindmate_server.review.domain.Tag;
import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import com.mindmate.mindmate_server.review.repository.ReviewRedisRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewRedisRepository reviewRedisRepository;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private AdminReviewServiceImpl adminReviewService;

    @Mock
    private Review review;

    @Mock
    private Profile profile;

    @Mock
    private EvaluationTag evaluationTag1;

    @Mock
    private EvaluationTag evaluationTag2;

    private List<Review> reviews;

    @BeforeEach
    void setUp() {
        // Common mock setup for all tests
        when(review.getId()).thenReturn(1L);
        when(review.getReviewedProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);

        when(review.getRating()).thenReturn(5);

        List<EvaluationTag> tags = new ArrayList<>();
        when(evaluationTag1.getTagContent()).thenReturn(Tag.RESPONSIVE);
        when(evaluationTag2.getTagContent()).thenReturn(Tag.EMPATHETIC);
        tags.add(evaluationTag1);
        tags.add(evaluationTag2);
        when(review.getReviewTags()).thenReturn(tags);

        reviews = Arrays.asList(review);
    }

    @Nested
    @DisplayName("getReviews Tests")
    class GetReviewsTests {

        @Test
        @DisplayName("모든 리뷰 조회")
        void getReviews_all() {
            // Given
            int page = 0;
            int size = 10;
            Integer minRating = null;
            Integer maxRating = null;
            Boolean reported = null;

            Page<Review> reviewPage = new PageImpl<>(reviews);
            when(reviewRepository.findReviewsWithFilters(eq(minRating), eq(maxRating), any(Pageable.class)))
                    .thenReturn(reviewPage);

            // When
            Page<ReviewResponse> result = adminReviewService.getReviews(page, size, minRating, maxRating, reported);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(reviewRepository).findReviewsWithFilters(eq(minRating), eq(maxRating), any(Pageable.class));
            verify(reportService, never()).findReportedReviewIds();
        }

        @Test
        @DisplayName("평점 필터링된 리뷰 조회")
        void getReviews_withRatingFilter() {
            // Given
            int page = 0;
            int size = 10;
            Integer minRating = 3;
            Integer maxRating = 5;
            Boolean reported = null;

            Page<Review> reviewPage = new PageImpl<>(reviews);
            when(reviewRepository.findReviewsWithFilters(eq(minRating), eq(maxRating), any(Pageable.class)))
                    .thenReturn(reviewPage);

            // When
            Page<ReviewResponse> result = adminReviewService.getReviews(page, size, minRating, maxRating, reported);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(reviewRepository).findReviewsWithFilters(eq(minRating), eq(maxRating), any(Pageable.class));
            verify(reportService, never()).findReportedReviewIds();
        }

        @Test
        @DisplayName("신고된 리뷰만 조회")
        void getReviews_reportedOnly() {
            // Given
            int page = 0;
            int size = 10;
            Integer minRating = null;
            Integer maxRating = null;
            Boolean reported = true;

            List<Long> reportedIds = Arrays.asList(1L, 2L);
            when(reportService.findReportedReviewIds()).thenReturn(reportedIds);

            Page<Review> reviewPage = new PageImpl<>(reviews);
            when(reviewRepository.findReviewsWithFiltersAndReportCheck(eq(minRating), eq(maxRating), eq(reportedIds), any(Pageable.class)))
                    .thenReturn(reviewPage);

            // When
            Page<ReviewResponse> result = adminReviewService.getReviews(page, size, minRating, maxRating, reported);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(reportService).findReportedReviewIds();
            verify(reviewRepository).findReviewsWithFiltersAndReportCheck(eq(minRating), eq(maxRating), eq(reportedIds), any(Pageable.class));
        }

        @Test
        @DisplayName("필터링 조합 - 평점 + 신고")
        void getReviews_ratingAndReported() {
            // Given
            int page = 0;
            int size = 10;
            Integer minRating = 4;
            Integer maxRating = 5;
            Boolean reported = true;

            List<Long> reportedIds = Arrays.asList(1L, 2L);
            when(reportService.findReportedReviewIds()).thenReturn(reportedIds);

            Page<Review> reviewPage = new PageImpl<>(reviews);
            when(reviewRepository.findReviewsWithFiltersAndReportCheck(eq(minRating), eq(maxRating), eq(reportedIds), any(Pageable.class)))
                    .thenReturn(reviewPage);

            // When
            Page<ReviewResponse> result = adminReviewService.getReviews(page, size, minRating, maxRating, reported);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(reportService).findReportedReviewIds();
            verify(reviewRepository).findReviewsWithFiltersAndReportCheck(eq(minRating), eq(maxRating), eq(reportedIds), any(Pageable.class));
        }

        @Test
        @DisplayName("빈 결과 반환")
        void getReviews_emptyResult() {
            // Given
            int page = 0;
            int size = 10;
            Integer minRating = 1;
            Integer maxRating = 5;
            Boolean reported = null;

            Page<Review> emptyPage = new PageImpl<>(new ArrayList<>());
            when(reviewRepository.findReviewsWithFilters(eq(minRating), eq(maxRating), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            Page<ReviewResponse> result = adminReviewService.getReviews(page, size, minRating, maxRating, reported);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
            verify(reviewRepository).findReviewsWithFilters(eq(minRating), eq(maxRating), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("deleteReview Tests")
    class DeleteReviewTests {

        @Test
        @DisplayName("리뷰 삭제 성공")
        void deleteReview_success() {
            // Given
            Long reviewId = 1L;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

            // When
            assertDoesNotThrow(() -> adminReviewService.deleteReview(reviewId));

            // Then
            verify(reviewRepository).findById(reviewId);
            verify(profile).decrementCounselingCount();
            verify(profile).subtractRating(anyDouble());
            verify(reviewRedisRepository).decrementTagCount(eq(profile.getId()), eq(Tag.RESPONSIVE.getContent()));
            verify(reviewRedisRepository).decrementTagCount(eq(profile.getId()), eq(Tag.EMPATHETIC.getContent()));
            verify(reviewRepository).delete(review);
            verify(reviewRedisRepository).deleteAllProfileCaches(profile.getId());
        }

        @Test
        @DisplayName("리뷰 삭제 실패 - 리뷰 없음")
        void deleteReview_reviewNotFound() {
            // Given
            Long reviewId = 99L;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            // When & Then
            CustomException exception = assertThrows(CustomException.class, () -> {
                adminReviewService.deleteReview(reviewId);
            });

            assertEquals(ReviewErrorCode.REVIEW_NOT_FOUND, exception.getErrorCode());
            verify(reviewRepository).findById(reviewId);
            verify(reviewRepository, never()).delete(any(Review.class));
        }

        @Test
        @DisplayName("리뷰 삭제 후 프로필 메트릭 조정")
        void deleteReview_adjustsProfileMetrics() {
            // Given
            Long reviewId = 1L;
            int rating = 4;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(review.getRating()).thenReturn(rating);

            // When
            adminReviewService.deleteReview(reviewId);

            // Then
            verify(profile).decrementCounselingCount();
            verify(profile).subtractRating(eq((double)rating));
        }

        @Test
        @DisplayName("리뷰 삭제 후 태그 카운트 조정")
        void deleteReview_adjustsTagCounts() {
            // Given
            Long reviewId = 1L;
            Long profileId = 1L;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(profile.getId()).thenReturn(profileId);

            // When
            adminReviewService.deleteReview(reviewId);

            // Then
            verify(reviewRedisRepository).decrementTagCount(profileId, Tag.RESPONSIVE.getContent());
            verify(reviewRedisRepository).decrementTagCount(profileId, Tag.EMPATHETIC.getContent());
        }

        @Test
        @DisplayName("리뷰 삭제 후 레디스 캐시 제거")
        void deleteReview_clearsCaches() {
            // Given
            Long reviewId = 1L;
            Long profileId = 1L;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
            when(profile.getId()).thenReturn(profileId);

            // When
            adminReviewService.deleteReview(reviewId);

            // Then
            verify(reviewRedisRepository).deleteAllProfileCaches(profileId);
        }
    }
}