package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.review.dto.ReviewResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseProfileResponse {
    private Long id;
    private String nickname;
    private String profileImage;
    private LocalDateTime createdAt;
    private Integer totalCounselingCount;
    private Double averageRating;
    private List<ReviewResponse> reviews;
}