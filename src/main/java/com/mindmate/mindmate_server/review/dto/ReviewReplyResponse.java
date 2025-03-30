package com.mindmate.mindmate_server.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyResponse {
    private Long reviewId;
    private String content;
    private LocalDateTime createdAt;
}
