package com.mindmate.mindmate_server.point.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointBalanceResponse {
    private Long userId;
    private Integer balance;
}