package com.mindmate.mindmate_server.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long reportId;
    private String message;
}
