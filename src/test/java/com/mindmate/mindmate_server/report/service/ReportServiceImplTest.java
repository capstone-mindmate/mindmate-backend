package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReportErrorCode;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {
    @Mock private UserService userService;
    @Mock private MatchingService matchingService;
    @Mock private ChatRoomService chatRoomService;
    @Mock private ReviewService reviewService;
    @Mock private AdminUserSuspensionService suspensionService;
    @Mock private MagazineService magazineService;
    @Mock private SlackNotifier slackNotifier;
    @Mock private ReportRepository reportRepository;


    @InjectMocks
    private ReportServiceImpl reportService;

    private static final Long REPORTER_ID = 1L;
    private static final Long REPORTED_USER_ID = 2L;
    private static final Long ADMIN_USER_ID = 3L;
    private static final Long TARGET_ID = 100L;
    private static final Long REPORT_ID = 200L;

    private User mockReporter;
    private User mockReportedUser;
    private User mockAdminUser;
    private Profile mockReporterProfile;
    private Profile mockReportedUserProfile;
    private ReportRequest reportRequest;

    @BeforeEach
    void setup() {
        setupMockUsers();
        setupMockProfiles();
        setupUserServiceMocks();
        setupReportRequest();
    }

    private void setupMockUsers() {
        mockReporter = mock(User.class);
        mockReportedUser = mock(User.class);
        mockAdminUser = mock(User.class);

        when(mockReporter.getId()).thenReturn(REPORTER_ID);
        when(mockReportedUser.getId()).thenReturn(REPORTED_USER_ID);
        when(mockAdminUser.getId()).thenReturn(ADMIN_USER_ID);

        when(mockReporter.getCurrentRole()).thenReturn(RoleType.ROLE_USER);
        when(mockReportedUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);
        when(mockAdminUser.getCurrentRole()).thenReturn(RoleType.ROLE_ADMIN);
    }

    private void setupMockProfiles() {
        mockReporterProfile = mock(Profile.class);
        mockReportedUserProfile = mock(Profile.class);

        when(mockReporter.getProfile()).thenReturn(mockReporterProfile);
        when(mockReportedUser.getProfile()).thenReturn(mockReportedUserProfile);
        when(mockReporterProfile.getNickname()).thenReturn("Reporter");
        when(mockReportedUserProfile.getNickname()).thenReturn("ReportedUser");
    }

    private void setupUserServiceMocks() {
        when(userService.findUserById(REPORTER_ID)).thenReturn(mockReporter);
        when(userService.findUserById(REPORTED_USER_ID)).thenReturn(mockReportedUser);
        when(userService.findUserById(ADMIN_USER_ID)).thenReturn(mockAdminUser);
    }

    private void setupReportRequest() {
        reportRequest = ReportRequest.builder()
                .reportedUserId(REPORTED_USER_ID)
                .reportReason(ReportReason.ABUSIVE_LANGUAGE)
                .additionalComment("신고 내용입니다")
                .reportTarget(ReportTarget.MATCHING)
                .targetId(TARGET_ID)
                .build();
    }

    private Report createMockReport() {
        Report report = mock(Report.class);
        when(report.getId()).thenReturn(REPORT_ID);
        when(report.getReporter()).thenReturn(mockReporter);
        when(report.getReportedUser()).thenReturn(mockReportedUser);
        when(report.getReportReason()).thenReturn(ReportReason.ABUSIVE_LANGUAGE);
        when(report.getReportTarget()).thenReturn(ReportTarget.MATCHING);
        when(report.getTargetId()).thenReturn(TARGET_ID);
        when(report.getAdditionalComment()).thenReturn("신고 내용입니다");
        when(report.getCreatedAt()).thenReturn(LocalDateTime.now());
        return report;
    }

    @Nested
    @DisplayName("신고 생성/중복/자기자신 테스트")
    class CreateReportParamTest {
        @Test
        @DisplayName("정상적인 신고 생성 성공")
        void createReport_Success() {
            // given
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(mockReportedUser.getReportCount()).thenReturn(1);

            Report mockReport = createMockReport();
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);

            // when
            ReportResponse response = reportService.createReport(REPORTER_ID, reportRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getReportId()).isEqualTo(REPORT_ID);
            assertThat(response.getMessage()).isEqualTo("신고가 접수되었습니다.");

            verify(slackNotifier).sendReportAlert(mockReport);
            verify(mockReportedUser).incrementReportCount();
            verify(userService).save(mockReportedUser);
            verify(matchingService).findMatchingById(TARGET_ID);
        }

        @Test
        @DisplayName("관리자 신고 시 예외 발생")
        void createReport_AdminUser_ThrowsException() {
            // given
            ReportRequest adminReportRequest = reportRequest.toBuilder()
                    .reportedUserId(ADMIN_USER_ID)
                    .build();

            // when & then
            CustomException exception = assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, adminReportRequest));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.ADMIN_SUSPENSION_NOT_ALLOWED);
            verify(reportRepository, never()).save(any());
            verify(slackNotifier, never()).sendReportAlert(any());
        }

        @ParameterizedTest
        @DisplayName("중복 신고 및 자기 자신 신고 예외")
        @MethodSource("invalidReportScenarios")
        void createReport_InvalidScenarios(boolean isDuplicate, boolean isSelf, Class<? extends Exception> expectedException) {
            // given
            Long targetUserId = isSelf ? REPORTER_ID : REPORTED_USER_ID;
            ReportRequest invalidRequest = reportRequest.toBuilder()
                    .reportedUserId(targetUserId)
                    .build();

            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(isDuplicate);

            // when & then
            assertThrows(expectedException, () -> reportService.createReport(REPORTER_ID, invalidRequest));
            verify(reportRepository, never()).save(any());
            verify(slackNotifier, never()).sendReportAlert(any());
        }

        static Stream<Arguments> invalidReportScenarios() {
            return Stream.of(
                    Arguments.of(true, false, CustomException.class),   // 중복 신고
                    Arguments.of(false, true, CustomException.class)   // 자기 자신
            );
        }
    }

    @Nested
    @DisplayName("신고 임계치 및 정지 처리 테스트")
    class ReportThresholdTest {
        @ParameterizedTest
        @DisplayName("신고 횟수에 따른 정지 처리")
        @MethodSource("thresholdScenarios")
        void checkReportThreshold_ApplySuspension(int reportCount, boolean expectWeakSuspension, boolean expectStrongSuspension) {
            // given
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(mockReportedUser.getReportCount()).thenReturn(reportCount);

            Report mockReport = createMockReport();
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);

            // when
            reportService.createReport(REPORTER_ID, reportRequest);

            // then
            if (expectStrongSuspension) {
                verify(suspensionService).suspendUser(
                        eq(REPORTED_USER_ID),
                        eq(reportCount),
                        eq(Duration.ofDays(30)),
                        contains("신고 누적")
                );
            } else if (expectWeakSuspension) {
                verify(suspensionService).suspendUser(
                        eq(REPORTED_USER_ID),
                        eq(reportCount),
                        eq(Duration.ofDays(3)),
                        contains("신고 누적")
                );
            } else {
                verify(suspensionService, never()).suspendUser(anyLong(), anyInt(), any(), anyString());
            }
        }

        static Stream<Arguments> thresholdScenarios() {
            return Stream.of(
                    Arguments.of(3, false, false),   // 임계치 미달
                    Arguments.of(5, true, false),    // 약한 임계치
                    Arguments.of(7, true, false),    // 약한 임계치 범위
                    Arguments.of(10, false, true),   // 강한 임계치
                    Arguments.of(15, false, true)    // 강한 임계치 초과
            );
        }

        @Test
        @DisplayName("정지 처리 시 올바른 메시지 포맷")
        void applySuspension_CorrectMessageFormat() {
            // given
            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(mockReportedUser.getReportCount()).thenReturn(10);

            Report mockReport = createMockReport();
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);

            // when
            reportService.createReport(REPORTER_ID, reportRequest);

            // then
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(suspensionService).suspendUser(
                    eq(REPORTED_USER_ID),
                    eq(10),
                    eq(Duration.ofDays(30)),
                    messageCaptor.capture()
            );

            String capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage).isEqualTo("신고 누적 (신고 횟수: 10)");
        }
    }

    @Nested
    @DisplayName("신고 대상 검증 테스트")
    class ValidateTargetExistTest {
        @ParameterizedTest
        @DisplayName("신고 대상별 존재 여부 검증")
        @MethodSource("targetValidationScenarios")
        void validateTargetExists_AllTargetTypes(ReportTarget target, String serviceName) {
            // given
            ReportRequest targetRequest = reportRequest.toBuilder()
                    .reportTarget(target)
                    .build();

            when(reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(any(), any(), any(), any())).thenReturn(false);
            when(mockReportedUser.getReportCount()).thenReturn(1);

            Report mockReport = createMockReport();
            when(reportRepository.save(any(Report.class))).thenReturn(mockReport);

            // when
            reportService.createReport(REPORTER_ID, targetRequest);

            // then
            switch (target) {
                case MATCHING:
                    verify(matchingService).findMatchingById(TARGET_ID);
                    break;
                case CHATROOM:
                    verify(chatRoomService).findChatRoomById(TARGET_ID);
                    break;
                case REVIEW:
                    verify(reviewService).findReviewById(TARGET_ID);
                    break;
                case MAGAZINE:
                    verify(magazineService).findMagazineById(TARGET_ID);
                    break;
                case PROFILE:
                    break;
            }
        }

        static Stream<Arguments> targetValidationScenarios() {
            return Stream.of(
                    Arguments.of(ReportTarget.MATCHING, "matchingService"),
                    Arguments.of(ReportTarget.CHATROOM, "chatRoomService"),
                    Arguments.of(ReportTarget.REVIEW, "reviewService"),
                    Arguments.of(ReportTarget.MAGAZINE, "magazineService"),
                    Arguments.of(ReportTarget.PROFILE, "none")
            );
        }

        @Test
        @DisplayName("존재하지 않는 대상 신고 시 예외 발생")
        void validateTargetExists_NotFound_ThrowsException() {
            // given
            when(matchingService.findMatchingById(TARGET_ID))
                    .thenThrow(new CustomException(ReportErrorCode.REPORT_NOT_FOUND));

            // when & then
            assertThrows(CustomException.class, () -> reportService.createReport(REPORTER_ID, reportRequest));
            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("신고 조회 테스트")
    class FindReportTest {
        @Test
        @DisplayName("ID로 신고 조회 성공")
        void findReportById_Success() {
            // given
            Report mockReport = createMockReport();
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(mockReport));

            // when
            Report result = reportService.findReportById(REPORT_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(mockReport);
            verify(reportRepository).findById(REPORT_ID);
        }

        @Test
        @DisplayName("존재하지 않은 신고 조회 실패")
        void findReportById_Failure() {
            // given
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

            // when & then
            assertThrows(CustomException.class, () -> reportService.findReportById(REPORT_ID));
        }

        @Test
        @DisplayName("신고된 리뷰 ID 목록 조회")
        void findReportedReviewIds_Success() {
            // given
            List<Long> expectedIds = Arrays.asList(1L, 2L, 3L);
            when(reportRepository.findReportedReviewIds()).thenReturn(expectedIds);

            // when
            List<Long> result = reportService.findReportedReviewIds();

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyElementsOf(expectedIds);
            verify(reportRepository).findReportedReviewIds();
        }
    }
}