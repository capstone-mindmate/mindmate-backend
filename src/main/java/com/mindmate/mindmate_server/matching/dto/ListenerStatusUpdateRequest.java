package com.mindmate.mindmate_server.matching.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListenerStatusUpdateRequest {
    @NotNull(message = "상태 값은 필수입니다")
    private ListenerStatus status;
}
