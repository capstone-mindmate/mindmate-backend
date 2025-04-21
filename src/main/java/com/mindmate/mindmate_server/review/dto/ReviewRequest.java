package com.mindmate.mindmate_server.review.dto;

import com.mindmate.mindmate_server.review.domain.EvaluationTag;
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
    private int rating;
    private String comment;
    private List<String> tags; // 프론트엔드에서 문자열로 받기
}