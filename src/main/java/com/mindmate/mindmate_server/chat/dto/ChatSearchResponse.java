package com.mindmate.mindmate_server.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSearchResponse {
    private List<Long> matchedMessageIds;
    private int totalMatches;
    private Long firstVisibleMatchId;
}
