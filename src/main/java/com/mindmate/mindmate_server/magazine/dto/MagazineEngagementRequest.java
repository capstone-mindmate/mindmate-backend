package com.mindmate.mindmate_server.magazine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MagazineEngagementRequest {
    private Long dwellTime; // 체류 시간
    private Double scrollPercentage; // 스크롤 얼마나 깊이 내렸는지
}
