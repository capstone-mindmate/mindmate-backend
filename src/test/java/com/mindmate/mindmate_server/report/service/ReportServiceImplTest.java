package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.MatchingErrorCode;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
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

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {
    @Mock private UserService userService;
    @Mock private MatchingService matchingService;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ReportRepository reportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Long reporterId;
    private Long reportedUserId;
    private User mockReporter;
    private User mockReportedUser;
    private Profile mockReporterProfile;
    private Profile mockReportedUserProfile;
    private ReportRequest reportRequest;

    @BeforeEach
    void setup() {
        reporterId = 1L;
        reportedUserId = 2L;

        mockReporter = mock(User.class);
        mockReportedUser = mock(User.class);
        mockReporterProfile = mock(Profile.class);
        mockReportedUserProfile = mock(Profile.class);

        when(mockReporter.getId()).thenReturn(reporterId);
        when(mockReportedUser.getId()).thenReturn(reportedUserId);
        when(mockReporter.getProfile()).thenReturn(mockReporterProfile);
        when(mockReportedUser.getProfile()).thenReturn(mockReportedUserProfile);
        when(userService.findUserById(reporterId)).thenReturn(mockReporter);
        when(userService.findUserById(reportedUserId)).thenReturn(mockReportedUser);

        reportRequest = ReportRequest.builder()
                .reportedUserId(reportedUserId)
                .reportReason(ReportReason.ABUSIVE_LANGUAGE)
                .additionalComment("신고 내용입니다")
                .reportTarget(ReportTarget.MATCHING)
                .targetId(100L)
                .build();


    }

    @Nested
    @DisplayName("신고 생성 테스트")
    class CreateReportTest {
        @Test
        @DisplayName("신고 생성 성공")
        void createReport_Success() {
            // given
            Report mockReport = mock(Report.class);

            when(mockReport.getId()).thenReturn(100L);
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(mockReporter, mockReportedUser, ReportTarget.MATCHING, 100L)).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);

            // when
            ReportResponse response = reportService.createReport(reporterId, reportRequest);

            // then
            assertNotNull(response);
            assertEquals(100L, response.getReportId());
            assertEquals("신고가 접수되었습니다.", response.getMessage());
            verify(mockReportedUser).incrementReportCount();
            verify(userService).save(mockReportedUser);
            verify(matchingService).findMatchingById(100L);
        }

        @Test
        @DisplayName("자기 자신 신고 실패")
        void createReport_Failure_SelfReport() {
            // given
            ReportRequest selfReportRequest = ReportRequest.builder()
                    .reportedUserId(reporterId) // 자기 자신을 신고
                    .reportReason(ReportReason.ABUSIVE_LANGUAGE)
                    .additionalComment("신고 내용입니다")
                    .reportTarget(ReportTarget.MATCHING)
                    .targetId(100L)
                    .build();

            // when & then
            assertThrows(CustomException.class, () -> reportService.createReport(reporterId, selfReportRequest));
            verify(reportRepository, never()).save(any(Report.class));
        }

        @Test
        @DisplayName("중복 신고 실패")
        void createReport_Failure_DuplicateReport() {
            // given
            Report mockReport = mock(Report.class);
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(mockReporter, mockReportedUser, ReportTarget.MATCHING, 100L)).thenReturn(true);

            // when & then
            assertThrows(CustomException.class, () -> reportService.createReport(reporterId, reportRequest));
            verify(reportRepository, never()).save(any(Report.class));
        }

        @Test
        @DisplayName("신고 대상 존재하지 않음 실패")
        void createReport_Failure_TargetNotFound() {
            // given
            when(matchingService.findMatchingById(100L)).thenThrow(new CustomException(MatchingErrorCode.MATCHING_NOT_FOUND));

            // when & then
            assertThrows(CustomException.class, () -> reportService.createReport(reporterId, reportRequest));
            verify(reportRepository, never()).save(any(Report.class));
        }
    }

    @Nested
    @DisplayName("신고 임계치 테스트")
    class ReportThresholdTest {
        @Test
        @DisplayName("약한 임계치 도달 시 짧은 정지")
        void checkReportThreshold_WeakThreshold() {
            // given
            Report mockReport = mock(Report.class);
            when(mockReport.getId()).thenReturn(100L);
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);
            when(mockReportedUser.getReportCount()).thenReturn((int) ReportServiceImpl.REPORT_COUNT_WEAK_THRESHOLD);

            // when
            reportService.createReport(reporterId, reportRequest);

            // then
            verify(mockReportedUser).suspend(Duration.ofDays(ReportServiceImpl.REPORT_COUNT_WEAK_SUSPEND));
            verify(userService).save(mockReportedUser);
        }

        @Test
        @DisplayName("강한 임계치 도달 시 긴 정지")
        void checkReportThreshold_StrongThreshold() {
            // given
            Report mockReport = mock(Report.class);
            when(mockReport.getId()).thenReturn(100L);
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);
            when(mockReportedUser.getReportCount()).thenReturn((int) ReportServiceImpl.REPORT_COUNT_STRONG_THRESHOLD);

            // when
            reportService.createReport(reporterId, reportRequest);

            // then
            verify(mockReportedUser).suspend(Duration.ofDays(ReportServiceImpl.REPORT_COUNT_STRONG_SUSPEND));
            verify(userService).save(mockReportedUser);
        }
    }

    @Nested
    @DisplayName("신고 조회 테스트")
    class FindReportTest {
        @Test
        @DisplayName("ID로 신고 조회 성공")
        void findReportById_Success() {
            // given
            Long reportId = 100L;
            Report mockReport = mock(Report.class);
            when(reportRepository.findById(reportId)).thenReturn(Optional.of(mockReport));

            // when
            Report result = reportService.findReportById(reportId);

            // then
            assertNotNull(result);
            assertEquals(mockReport, result);
        }

        @Test
        @DisplayName("존재하지 않은 신고 조회 실패")
        void findReportById_Failure() {
            // given
            Long reportId = 999L;
            when(reportRepository.findById(reportId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class, () -> reportService.findReportById(reportId));
        }
    }

}