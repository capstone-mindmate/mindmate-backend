package com.mindmate.mindmate_server.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingAcceptedEvent {
    private Long matchingId;
    private Long creatorId;
    private Long acceptedUserId;
    private List<Long> pendingWaitingUserIds; // 거절할 대기자s
}
