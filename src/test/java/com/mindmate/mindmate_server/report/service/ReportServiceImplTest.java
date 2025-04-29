package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportReason;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
import com.mindmate.mindmate_server.report.repository.ReportRepository;
import com.mindmate.mindmate_server.review.service.ReviewService;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.AdminUserSuspensionService;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @Mock private ReportRepository reportRepository;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ReviewService reviewService;
    @Mock private AdminUserSuspensionService suspensionService;


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

        when(mockReporter.getCurrentRole()).thenReturn(RoleType.ROLE_USER);
        when(mockReportedUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);
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
    @DisplayName("신고 생성/중복/자기자신 테스트")
    class CreateReportParamTest {
        @ParameterizedTest(name = "중복: {0}, 자기자신: {1}, 기대: {2}")
        @CsvSource({
                "false,false,true",   // 정상
                "true,false,false",   // 중복
                "false,true,false"    // 자기자신
        })
        void createReport_Param(boolean isDuplicate, boolean isSelf, boolean expectSuccess) {
            Long targetId = 100L;
            Long reportedId = isSelf ? reporterId : reportedUserId;

            ReportRequest req = reportRequest.toBuilder().reportedUserId(reportedId).build();

            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any()))
                    .thenReturn(isDuplicate);

            if (expectSuccess) {
                Report mockReport = mock(Report.class);
                when(mockReport.getId()).thenReturn(100L);
                when(reportRepository.save(any(Report.class))).thenReturn(mockReport);

                ReportResponse response = reportService.createReport(reporterId, req);
                assertNotNull(response);
            } else {
                assertThrows(CustomException.class, () -> reportService.createReport(reporterId, req));
                verify(reportRepository, never()).save(any(Report.class));
            }
        }
    }


    @Nested
    @DisplayName("신고 임계치 테스트")
    class ReportThresholdTest {
        @ParameterizedTest(name = "임계치: {0}, 정지일: {1}")
        @CsvSource({
                "5,3",   // 약한 임계치
                "10,30"  // 강한 임계치
        })
        void checkReportThreshold_Param(int reportCount, int suspendDays) {
            Report mockReport = mock(Report.class);
            when(mockReport.getId()).thenReturn(100L);
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);
            when(mockReportedUser.getReportCount()).thenReturn(reportCount);

            reportService.createReport(reporterId, reportRequest);

            verify(suspensionService).suspendUser(
                    eq(mockReportedUser.getId()),
                    eq(reportCount),
                    eq(Duration.ofDays(suspendDays)),
                    anyString()
            );
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