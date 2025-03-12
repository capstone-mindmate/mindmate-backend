package com.mindmate.mindmate_server.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

// 평가?아님 직접?
@Getter
@Builder
public class EvaluationTagRequest {
    private Set<String> tags;
}

