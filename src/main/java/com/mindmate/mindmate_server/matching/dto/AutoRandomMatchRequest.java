package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import lombok.Data;

@Data
public class AutoRandomMatchRequest {

    private Long profileId;
    private InitiatorType initiatorType;
}
