package com.mindmate.mindmate_server.user.domain;

public enum Badge {
    NONE("일반 상담사"),
    POPULAR("인기 상담사"),  // 상담 횟수 및 평점
    EXPERT("전문 상담사");    // 자격 인증 완료

    private final String badgeName;

    Badge(String badgeName) {
        this.badgeName = badgeName;
    }

    public String getBadgeName() {
        return badgeName;
    }
}