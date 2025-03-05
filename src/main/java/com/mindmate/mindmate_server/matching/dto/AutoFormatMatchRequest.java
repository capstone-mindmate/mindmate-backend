package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import lombok.*;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoFormatMatchRequest {
    private Long profileId;
    private InitiatorType initiatorType;
    private Set<CounselingField> requestedFields;
    private CounselingStyle preferredStyle;
}