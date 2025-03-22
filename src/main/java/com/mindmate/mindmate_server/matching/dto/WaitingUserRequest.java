package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WaitingUserRequest {

    @Size(max = 100)
    private String message;
    private boolean anonymous;
}
