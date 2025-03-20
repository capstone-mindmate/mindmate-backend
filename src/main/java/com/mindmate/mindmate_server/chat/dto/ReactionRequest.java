package com.mindmate.mindmate_server.chat.dto;

import com.mindmate.mindmate_server.chat.domain.ReactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReactionRequest {
    private Long messageId;
    private Long roomId;
    private ReactionType reactionType;
}
