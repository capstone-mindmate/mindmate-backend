package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualMatchRequest {
    private Long initiatorId;
    private InitiatorType initiatorType;
    private Long recipientId;
    private Set<CounselingField> requestedFields;
}
