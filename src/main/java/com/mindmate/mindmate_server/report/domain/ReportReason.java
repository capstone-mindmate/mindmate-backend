package com.mindmate.mindmate_server.report.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    ABUSIVE_LANGUAGE("욕설, 폭언, 비방 및 혐오표현을 사용해요"),
    SEXUAL_HARASSMENT("성적 수치심을 유발하거나 노출해요"),
    SPAM_OR_REPETITIVE("도배 또는 반복적인 내용이에요"),
    MALICIOUS_LINK("스팸 또는 악성 링크가 포함되어 있어요"),
    EXCESSIVE_PROMOTION("상업적 목적의 과도한 홍보예요"),
    PERSONAL_INFO_VIOLATION("개인정보를 불법으로 요구하거나 유출했어요"),
    ILLEGAL_CONTENT("불법 정보 또는 행위를 조장해요"),
    OTHER("기타 문제가 있어 신고하고 싶어요");

    private final String description;
}
