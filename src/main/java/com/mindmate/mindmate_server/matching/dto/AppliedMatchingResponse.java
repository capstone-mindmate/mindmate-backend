package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.domain.WaitingUser;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppliedMatchingResponse {
    private MatchingResponse matching;
    private String applicationMessage;

    public static AppliedMatchingResponse of(Matching matching, WaitingUser waitingUser) {
        return AppliedMatchingResponse.builder()
                .matching(MatchingResponse.of(matching))
                .applicationMessage(waitingUser.getMessage())
                .build();
    }
}