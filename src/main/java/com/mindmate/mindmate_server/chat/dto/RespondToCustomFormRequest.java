package com.mindmate.mindmate_server.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondToCustomFormRequest {
    @NotNull
    private Long formId;

    @NotNull
    private Long chatRoomId;

    @NotEmpty
    private List<String> answers;
}
