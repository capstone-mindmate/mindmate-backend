package com.mindmate.mindmate_server.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MagazineEngagementEvent {
    private Long userId;
    private Long magazineId;
    private Long dwellTime; // 체류 시간
    private Double scrollPercentage; // 스크롤 얼마나 깊이 내렸는지
    private Instant timestamp;
}
