package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerStatusUpdateRequest {
    @NotNull(message = "가용성 값은 필수입니다")
    private boolean available;
    private Set<CounselingField> preferredFields;
    private CounselingStyle preferredStyle;
}
