package com.mindmate.mindmate_server.review.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Tag {
    RESPONSIVE("응답이 빨라요"),
    EMPATHETIC("공감을 잘해줘요"),
    TRUSTWORTHY("신뢰할 수 있는 대화였어요"),
    FRESH_PERSPECTIVE("새로운 관점을 제시해줘요"),
    DETAILED_ADVICE("솔직하고 현실적인 조언을 해줘요"),
    COMFORTABLE("편안한 분위기에서 이야기할 수 있었어요"),
    CLEAR_COMMUNICATION("의사소통이 명확해요"),
    OPEN_MINDED("열린 마음으로 대화해요"),
    RESPECTFUL("존중하는 태도로 대화해요");

    private final String content;

    public static Tag fromContent(String content) {
        return Arrays.stream(values())
                .filter(tag -> tag.getContent().equals(content))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid tag content: " + content));
    }
}