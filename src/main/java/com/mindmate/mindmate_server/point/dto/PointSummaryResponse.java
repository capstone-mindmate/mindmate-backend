package com.mindmate.mindmate_server.point.dto;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PointSummaryResponse {
    private int currentBalance;
    private int totalEarned;
    private int totalSpent;
}