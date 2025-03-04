package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchingActionRequest { // 수락, 취소, 완료
    @NotNull(message = "프로필 ID는 필수입니다")
    private Long profileId;

    @NotNull(message = "프로필 타입은 필수입니다")
    private InitiatorType profileType;
}