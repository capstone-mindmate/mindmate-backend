package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDetailResponse {
    private Long id;
    private Long userId;
    private String nickname;
    private String profileImage;
    private String department;
    private Integer entranceTime;
    private boolean graduation;

    private int totalCounselingCount;
    private int avgResponseTime;
    private Double averageRating;

    private Map<String, Integer> evaluationTagCounts;
    private Map<String, Integer> categoryCounts;

    private int speakerRoleCount;
    private int listenerRoleCount;

    private List<ReviewResponse> reviews;
    private int points;
    private LocalDateTime createdAt;
}