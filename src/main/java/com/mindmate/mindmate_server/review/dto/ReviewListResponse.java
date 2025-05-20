package com.mindmate.mindmate_server.review.dto;

import com.mindmate.mindmate_server.review.domain.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListResponse {
    private Long id;
    private String reviewerNickname;
    private String reviewerProfileImage;
    private int rating;
    private String comment;
    private List<String> tags;
    private String createdAt;

    public static ReviewListResponse from(Review review) {
        return ReviewListResponse.builder()
                .id(review.getId())
                .reviewerNickname(review.getReviewer().getProfile().getNickname())
                .reviewerProfileImage(review.getReviewer().getProfile().getProfileImage().getImageUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .tags(review.getReviewTags().stream()
                        .map(tag -> tag.getTagContent().getContent())
                        .collect(Collectors.toList()))
                .createdAt(review.getCreatedAt().toString())
                .build();
    }
}