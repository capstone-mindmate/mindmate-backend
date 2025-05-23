package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.chat.service.ChatRoomService;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReportErrorCode;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.magazine.service.MagazineService;
import com.mindmate.mindmate_server.matching.service.MatchingService;
import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
import com.mindmate.mindmate_server.report.repository.ReportRepository;
import com.mindmate.mindmate_server.review.service.ReviewService;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.AdminUserSuspensionService;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final UserService userService;
    private final MatchingService matchingService;
    private final ChatRoomService chatRoomService;
    private final ReviewService reviewService;
    private final AdminUserSuspensionService suspensionService;
    private final MagazineService magazineService;

    private final SlackNotifier slackNotifier;
    private final ReportRepository reportRepository;

    public static final long REPORT_COUNT_WEAK_THRESHOLD = 5;
    public static final long REPORT_COUNT_STRONG_THRESHOLD = 10;
    public static final long REPORT_COUNT_WEAK_SUSPEND = 3;
    public static final long REPORT_COUNT_STRONG_SUSPEND = 30;


    /**
     * 신고하기
     * 유효성 검사 -> 중복된 신고, 자기 자신 신고, 해당 id의 엔티티가 존재하는지 확인
     */
    @Override
    @Transactional
    public ReportResponse createReport(Long reporterId, ReportRequest request) {
        User reporter = userService.findUserById(reporterId);
        User reportedUser = userService.findUserById(request.getReportedUserId());

        if (reportedUser.getCurrentRole().equals(RoleType.ROLE_ADMIN)) {
            throw new CustomException(UserErrorCode.ADMIN_SUSPENSION_NOT_ALLOWED);
        }

        checkDuplicateReport(reporter, reportedUser, request.getReportTarget(), request.getTargetId());
        validateReportRequest(reporter.getId(), reportedUser.getId());
        validateTargetExists(request.getReportTarget(), request.getTargetId());

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportReason(request.getReportReason())
                .additionalComment(request.getAdditionalComment())
                .reportTarget(request.getReportTarget())
                .targetId(request.getTargetId())
                .build();

        Report savedReport = reportRepository.save(report);

        slackNotifier.sendReportAlert(savedReport);
        reportedUser.incrementReportCount();
        checkReportThreshold(reportedUser);
        userService.save(reportedUser);

        return new ReportResponse(savedReport.getId(), "신고가 접수되었습니다.");
    }

    private void checkReportThreshold(User reportedUser) {
        if (reportedUser.getReportCount() >= REPORT_COUNT_STRONG_THRESHOLD) {
            applySuspension(reportedUser, REPORT_COUNT_STRONG_SUSPEND);
        } else if (reportedUser.getReportCount() >= REPORT_COUNT_WEAK_THRESHOLD) {
            applySuspension(reportedUser, REPORT_COUNT_WEAK_SUSPEND);
        }
    }

    private void applySuspension(User user, long suspensionDays) {
        suspensionService.suspendUser(
                user.getId(),
                user.getReportCount(),
                Duration.ofDays(suspensionDays),
                String.format("신고 누적 (신고 횟수: %d)", user.getReportCount())
        );
    }

    private void validateTargetExists(ReportTarget reportTarget, Long targetId) {
        switch (reportTarget) {
            case MATCHING:
                matchingService.findMatchingById(targetId);
                break;
            case CHATROOM:
                chatRoomService.findChatRoomById(targetId);
                break;
            case PROFILE:
                break;
            case REVIEW:
                reviewService.findReviewById(targetId);
                break;
            case MAGAZINE:
                magazineService.findMagazineById(targetId);
                break;
        }
    }

    private void validateReportRequest(Long reporterId, Long reportedUserId) {
        if (reporterId.equals(reportedUserId)) {
            throw new CustomException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
    }

    @Override
    public Report findReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));

    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findReportedReviewIds() {
        return reportRepository.findReportedReviewIds();
    }

    private void checkDuplicateReport(User reporter, User reportedUser, ReportTarget target, Long targetId) {
        boolean exists = reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(
                reporter, reportedUser, target, targetId);

        if (exists) {
            throw new CustomException(ReportErrorCode.DUPLICATE_REPORT);
        }
    }
}
