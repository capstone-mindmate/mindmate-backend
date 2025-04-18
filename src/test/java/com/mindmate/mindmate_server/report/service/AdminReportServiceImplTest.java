package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportDetailResponse;
import com.mindmate.mindmate_server.report.dto.ReportStatisticsResponse;
import com.mindmate.mindmate_server.report.dto.UnsuspendRequest;
import com.mindmate.mindmate_server.report.repository.ReportRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
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
import org.springframework.data.domain.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminReportServiceImplTest {
    @Mock private ReportRepository reportRepository;
    @Mock private ReportService reportService;
    @Mock private UserService userService;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    private TestReportBuilder reportBuilder;
    private User mockUser;
    private Long userId;

    @BeforeEach
    void setup() {
        userId = 1L;
        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(userService.findUserById(userId)).thenReturn(mockUser);

        reportBuilder = new TestReportBuilder();
        Report mockReport = reportBuilder.build();
        when(reportService.findReportById(anyLong())).thenReturn(mockReport);
    }

    private void setupMockPage(Pageable pageable, ReportTarget target, ReportReason reportReason) {
        Page<Report> mockPage = new PageImpl<>(List.of(reportBuilder.build()), pageable, 1);
        when(reportRepository.findReportsWithFilters(target, reportReason, pageable)).thenReturn(mockPage);
    }

    @Nested
    @DisplayName("신고 목록 조회 테스트")
    class GetReportsTest {
        @Test
        @DisplayName("필터링된 신고 목록 조회 성공")
        void getReports_WithFilters_Success() {
            // given
            int page = 0;
            int size = 10;
            ReportTarget target = ReportTarget.MATCHING;
            ReportReason reason = ReportReason.ABUSIVE_LANGUAGE;
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            setupMockPage(pageable, target, reason);

            // when
            Page<ReportDetailResponse> result = adminReportService.getReports(page, size, target, reason);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(reportRepository).findReportsWithFilters(target, reason, pageable);
        }

        @Test
        @DisplayName("필터 없이 신고 목록 조회 성공")
        void getReports_WithoutFilters_Success() {
            // given
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            setupMockPage(pageable, null, null);

            // when
            Page<ReportDetailResponse> result = adminReportService.getReports(page, size, null, null);

            // then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(reportRepository).findReportsWithFilters(null, null, pageable);
        }
    }

    @Nested
    @DisplayName("신고 상세 조회 테스트")
    class GetReportDetailTest {
        @Test
        @DisplayName("신고 상세 조회 성공")
        void getReportDetail_Success() {
            // given
            Long reportId = 100L;

            // when
            ReportDetailResponse response = adminReportService.getReportDetail(reportId);

            // then
            assertNotNull(response);
            assertEquals(reportId, response.getId());
            assertEquals("Reporter", response.getReporterName());
            assertEquals("ReportedUser", response.getReportedUserName());
            assertEquals(ReportReason.ABUSIVE_LANGUAGE, response.getReportReason());
            assertEquals(ReportTarget.MATCHING, response.getReportTarget());
            verify(reportService).findReportById(reportId);
        }
    }

    @Nested
    @DisplayName("사용자 정지 테스트")
    class SuspendUserTest {
//        @Test
//        @DisplayName("사용자 정지 성공")
//        void suspendUser_Success() {
//            // given
//            int reportCount = 5;
//            Duration duration = Duration.ofDays(3);
//
//            // when
//            adminReportService.suspendUser(userId, reportCount, duration);
//
//            // then
//            verify(mockUser).setReportCount(reportCount);
//            verify(mockUser).suspend(duration);
//            verify(userService).save(mockUser);
//        }
    }

    @Nested
    @DisplayName("사용자 정지 해제 테스트")
    class UnsuspendUserTest {
//        @Test
//        @DisplayName("사용자 정지 해제 성공")
//        void unsuspendUser_Success() {
//            // given
//            UnsuspendRequest request = UnsuspendRequest.builder().reportCount(0).build();
//
//            // when
//            adminReportService.unsuspendUser(userId, request);
//
//            // then
//            verify(mockUser).setReportCount(0);
//            verify(mockUser).unsuspend();
//            verify(userService).save(mockUser);
//        }
    }

    @Nested
    @DisplayName("신고 통계 조회 테스트")
    class GetStatisticsTest {
        @Test
        @DisplayName("날짜 범위 지정하여 통계 조회 성공")
        void getStatistics_WithDateRange_Success() {
            // given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);

            Map<ReportReason, Long> reasonStats = new HashMap<>();
            reasonStats.put(ReportReason.ABUSIVE_LANGUAGE, 5L);
            reasonStats.put(ReportReason.SPAM_OR_REPETITIVE, 3L);

            Map<ReportTarget, Long> targetStats = new HashMap<>();
            targetStats.put(ReportTarget.MATCHING, 4L);
            targetStats.put(ReportTarget.CHATROOM, 4L);

            when(reportRepository.countByCreatedAtBetween(start, end)).thenReturn(8L);
            when(reportRepository.countByReasonBetween(start, end)).thenReturn(reasonStats);
            when(reportRepository.countByTargetBetween(start, end)).thenReturn(targetStats);

            // when
            ReportStatisticsResponse response = adminReportService.getStatistics(startDate, endDate);

            // then
            assertNotNull(response);
            assertEquals(8L, response.getTotalReports());
            assertEquals(5L, response.getReportsByReason().get(ReportReason.ABUSIVE_LANGUAGE));
            assertEquals(4L, response.getReportsByTarget().get(ReportTarget.MATCHING));
            verify(reportRepository).countByCreatedAtBetween(start, end);
            verify(reportRepository).countByReasonBetween(start, end);
            verify(reportRepository).countByTargetBetween(start, end);
        }

        @Test
        @DisplayName("날짜 범위 미지정 시 기본값으로 통계 조회 성공")
        void getStatistics_WithDefaultDateRange_Success() {
            // given
            LocalDate defaultStart = LocalDate.now().minusMonths(1);
            LocalDate defaultEnd = LocalDate.now();
            LocalDateTime start = defaultStart.atStartOfDay();
            LocalDateTime end = defaultEnd.atTime(23, 59, 59);

            Map<ReportReason, Long> reasonStats = new HashMap<>();
            Map<ReportTarget, Long> targetStats = new HashMap<>();

            when(reportRepository.countByCreatedAtBetween(start, end)).thenReturn(0L);
            when(reportRepository.countByReasonBetween(start, end)).thenReturn(reasonStats);
            when(reportRepository.countByTargetBetween(start, end)).thenReturn(targetStats);

            // when
            ReportStatisticsResponse result = adminReportService.getStatistics(null, null);

            // then
            assertNotNull(result);
            assertEquals(0L, result.getTotalReports());
            verify(reportRepository).countByCreatedAtBetween(start, end);
        }

    }

    private static class TestReportBuilder {
        private Long id = 100L;
        private User reporter = mock(User.class);
        private User reportedUser = mock(User.class);
        private Profile reporterProfile = mock(Profile.class);
        private Profile reportedUserProfile = mock(Profile.class);
        private ReportReason reason = ReportReason.ABUSIVE_LANGUAGE;
        private ReportTarget target = ReportTarget.MATCHING;

        TestReportBuilder() {
            setupDefaultMocks();
        }

        private void setupDefaultMocks() {
            when(reporter.getId()).thenReturn(1L);
            when(reportedUser.getId()).thenReturn(2L);
            when(reporter.getProfile()).thenReturn(reporterProfile);
            when(reportedUser.getProfile()).thenReturn(reportedUserProfile);
            when(reporter.getReportCount()).thenReturn(0);
            when(reportedUser.getReportCount()).thenReturn(3);
            when(reporterProfile.getNickname()).thenReturn("Reporter");
            when(reportedUserProfile.getNickname()).thenReturn("ReportedUser");
            when(reporterProfile.getProfileImage()).thenReturn("reporter.jpg");
            when(reportedUserProfile.getProfileImage()).thenReturn("reported.jpg");
        }

        Report build() {
            Report report = mock(Report.class);
            when(report.getId()).thenReturn(id);
            when(report.getReporter()).thenReturn(reporter);
            when(report.getReportedUser()).thenReturn(reportedUser);
            when(report.getReportReason()).thenReturn(reason);
            when(report.getReportTarget()).thenReturn(target);
            when(report.getTargetId()).thenReturn(5L);
            when(report.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(report.getAdditionalComment()).thenReturn("Test comment");
            return report;
        }

        // 필요한 경우 setter 메서드 추가
        TestReportBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        TestReportBuilder withReason(ReportReason reason) {
            this.reason = reason;
            return this;
        }

        TestReportBuilder withTarget(ReportTarget target) {
            this.target = target;
            return this;
        }
    }
}