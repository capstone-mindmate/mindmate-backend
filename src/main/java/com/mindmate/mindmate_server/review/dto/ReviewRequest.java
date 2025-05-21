package com.mindmate.mindmate_server.review.dto;

import com.mindmate.mindmate_server.review.domain.EvaluationTag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    private Long chatRoomId;
    @Min(1) @Max(5)
    private int rating;
    @Size(max=200)
    private String comment;
    private List<String> tags;
    private boolean anonymous;
}