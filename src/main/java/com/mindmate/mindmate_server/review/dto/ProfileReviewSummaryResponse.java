package com.mindmate.mindmate_server.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileReviewSummaryResponse {
    private double averageRating;
    private int totalReviews;
    private Map<String, Integer> tagCounts;
    private List<ReviewListResponse> recentReviews;
}