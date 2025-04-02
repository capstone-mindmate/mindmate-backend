package com.mindmate.mindmate_server.review.dto;

import com.mindmate.mindmate_server.review.domain.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ReviewListResponse {
    private Long id;
    private String reviewerNickname;
    private int rating;
    private String comment;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static ReviewListResponse from(Review review) {
        return ReviewListResponse.builder()
                .id(review.getId())
                .reviewerNickname(review.getReviewer().getProfile().getNickname())
                .rating(review.getRating())
                .comment(review.getComment())
                .tags(review.getReviewTags().stream()
                        .map(tag -> tag.getTagContent().getContent())
                        .collect(Collectors.toList()))
                .createdAt(review.getCreatedAt())
                .build();
    }
}