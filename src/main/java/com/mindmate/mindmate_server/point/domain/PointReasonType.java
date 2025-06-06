package com.mindmate.mindmate_server.point.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PointReasonType {

    COUNSELING_PROVIDED("리스너 보상"),
    MAGAZINE_CREATED("매거진 선정"),
    REVIEW_WRITTEN("리뷰 작성"),
    ADMIN_GRANTED("관리자 승인"),

    COUNSELING_REQUESTED("스피커 요청"),
    EMOTICON_PURCHASED("이모티콘 구매"),
    PURCHASE("포인트 구매"),
    PROFILE_CREATED("프로필 생성");

    private final String title;
}