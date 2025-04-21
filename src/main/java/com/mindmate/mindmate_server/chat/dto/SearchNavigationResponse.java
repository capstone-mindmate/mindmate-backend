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
public class SearchNavigationResponse {
    private Long targetMessageId;
    private List<ChatMessageResponse> additionalMessages;
    private boolean hasMoreResults;
    private int currentMatchIndex;
    private int totalMatches;
}
