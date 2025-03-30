package com.mindmate.mindmate_server.review.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Tag {
    // 리스너 태그
    RESPONSIVE(TagType.LISTENER, "응답이 빨라요"),
    EMPATHETIC(TagType.LISTENER, "공감을 잘해줘요"),
    TRUSTWORTHY(TagType.LISTENER, "신뢰할 수 있는 대화였어요"),
    FRESH_PERSPECTIVE(TagType.LISTENER, "새로운 관점을 제시해줘요"),
    DETAILED_ADVICE(TagType.LISTENER, "솔직하고 현실적인 조언을 해줘요"),
    COMFORTABLE(TagType.LISTENER, "편안한 분위기에서 이야기할 수 있었어요"),

    // 스피커 태그
    CLEAR_COMMUNICATION(TagType.SPEAKER, "의사소통이 명확해요"),
    OPEN_MINDED(TagType.SPEAKER, "열린 마음으로 대화해요"),
    RESPECTFUL(TagType.SPEAKER, "존중하는 태도로 대화해요");

    private final TagType type;
    private final String content;

    public static Tag fromContent(String content) {
        return Arrays.stream(values())
                .filter(tag -> tag.getContent().equals(content))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid tag content: " + content));
    }
}