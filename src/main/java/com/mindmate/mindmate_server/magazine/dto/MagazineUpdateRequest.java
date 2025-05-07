package com.mindmate.mindmate_server.magazine.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MagazineUpdateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String subtitle;

    @NotNull
    private MatchingCategory category;

    @NotNull
    private List<MagazineContentDTO> contents;
}
