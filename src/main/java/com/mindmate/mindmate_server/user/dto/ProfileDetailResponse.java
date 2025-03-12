package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
    private LocalDateTime entranceTime;
    private boolean graduation;

    // 활동 정보
    private int totalCounselingCount;
    private int avgResponseTime;
    private Double averageRating;
    private Set<String> evaluationTags;

    // 리뷰 정보
    private List<ReviewResponse> reviews;
    private int points;
    private LocalDateTime createdAt;
}
