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
public class ReviewResponse {
    private Long id;
    private Long chatRoomId;
    private Long reviewerId;
    private String reviewerNickname;
    private String reviewerProfileImage;
    private int rating;
    private String comment;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        ReviewResponseBuilder response = ReviewResponse.builder()
                .id(review.getId())
                .chatRoomId(review.getChatRoom() != null ? review.getChatRoom().getId() : null)
                .reviewerId(review.getReviewer() != null ? review.getReviewer().getId() : null)
                .reviewerNickname(review.getReviewer() != null && review.getReviewer().getProfile() != null ?
                        review.getReviewer().getProfile().getNickname() : "Unknown")
                .reviewerProfileImage(review.getReviewer() != null && review.getReviewer().getProfile() != null ?
                        review.getReviewer().getProfile().getProfileImage() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt());

        if (review.getReviewTags() != null && !review.getReviewTags().isEmpty()) {
            List<String> tagContents = review.getReviewTags().stream()
                    .map(reviewTag -> reviewTag.getTagContent().getContent())
                    .collect(Collectors.toList());
            response.tags(tagContents);
        }

        return response.build();
    }
}
