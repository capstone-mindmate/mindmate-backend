package com.mindmate.mindmate_server.report.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportTarget {
    MATCHING,
    CHATROOM,
    PROFILE,
     MAGAZINE,
    REVIEW
}
