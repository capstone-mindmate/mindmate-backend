package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReportService {

    ReportResponse createReport(Long reporterId, ReportRequest request);

    Report findReportById(Long reportId);

    List<Long> findReportedReviewIds();
}
