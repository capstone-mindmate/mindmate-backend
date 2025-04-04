package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MagazineUpdateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private MatchingCategory category;
}
