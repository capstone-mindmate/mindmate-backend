package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.MatchingStatus;
import com.mindmate.mindmate_server.matching.domain.MatchingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingResponse {
    private Long id;
    private Long speakerProfileId;
    private Long listenerProfileId;
    // 이름 추가 할지 말지 결정
    private MatchingStatus status;
    private MatchingType type;
    private InitiatorType initiator;
    private Set<String> requestedFields;
    private LocalDateTime requestedAt;
    private LocalDateTime matchedAt;
    private LocalDateTime completedAt;
    private String chatRoomId;
    private String message;
}
