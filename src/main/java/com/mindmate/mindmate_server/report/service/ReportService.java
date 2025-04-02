package com.mindmate.mindmate_server.report.service;

import com.mindmate.mindmate_server.report.domain.Report;
import com.mindmate.mindmate_server.report.dto.ReportRequest;
import com.mindmate.mindmate_server.report.dto.ReportResponse;

public interface ReportService {

    ReportResponse createReport(Long reporterId, ReportRequest request);

    Report findReportById(Long reportId);
}
