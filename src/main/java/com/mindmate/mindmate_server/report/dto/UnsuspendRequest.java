package com.mindmate.mindmate_server.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnsuspendRequest {
    private int reportCount;

    public void setReportCount(Integer count) {
        this.reportCount = count;
    }
}
