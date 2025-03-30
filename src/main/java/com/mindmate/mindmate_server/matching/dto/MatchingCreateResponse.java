package com.mindmate.mindmate_server.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class MatchingCreateResponse {

    private Long matchingId;
    private Long chatRoomId;
}
