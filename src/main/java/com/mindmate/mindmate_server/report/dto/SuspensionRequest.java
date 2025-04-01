package com.mindmate.mindmate_server.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@Getter
@AllArgsConstructor
public class SuspensionRequest {
    private int reportCount;
    private int durationDays;

    public Duration getDuration() {
        return Duration.ofDays(durationDays);
    }
}
