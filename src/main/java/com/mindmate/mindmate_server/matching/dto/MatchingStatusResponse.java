package com.mindmate.mindmate_server.matching.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchingStatusResponse {
    private int currentActiveMatchings;
    private int maxActiveMatchings;
    private boolean canCreateMore;
}
