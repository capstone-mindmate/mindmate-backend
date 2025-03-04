package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoFormatMatchRequest {
    private Long profileId;
    private InitiatorType initiatorType;
    private Set<CounselingField> requestedFields;
    private CounselingStyle preferredStyle;
}