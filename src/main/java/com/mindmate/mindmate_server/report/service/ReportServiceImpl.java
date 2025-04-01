package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.ReportErrorCode;
import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.domain.ReportTarget;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
import com.mindmate.mindmate_server.report.repository.ReportRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final UserService userService;

    private final ReportRepository reportRepository;


    /**
     * 신고하기
     * 유효성 검사 -> 중복된 신고, 자기 자신 신고, 해당 id의 엔티티가 존재하는지 확인
     */
    @Override
    @Transactional
    public ReportResponse createReport(Long reporterId, ReportRequest request) {
        User reporter = userService.findUserById(reporterId);
        User reportedUser = userService.findUserById(request.getReportedUserId());

        checkDuplicateReport(reporter, reportedUser, request.getReportTarget(), request.getTargetId());

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportReason(request.getReportReason())
                .additionalComment(request.getAdditionalComment())
                .reportTarget(request.getReportTarget())
                .targetId(request.getTargetId())
                .build();

        Report savedReport = reportRepository.save(report);

        reportedUser.incrementReportCount();
        userService.save(reportedUser);

        return new ReportResponse(savedReport.getId(), "신고가 접수되었습니다.");
    }


    @Override
    public Report findReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ReportErrorCode.REPORT_NOT_FOUND));

    }

    private void checkDuplicateReport(User reporter, User reportedUser, ReportTarget target, String targetId) {
        boolean exists = reportRepository.existsByReporterAndReportedUserAndReportTargetAndTargetId(
                reporter, reportedUser, target, targetId);

        if (exists) {
            throw new CustomException(ReportErrorCode.DUPLICATE_REPORT);
        }
    }
}
