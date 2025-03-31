package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingUpdateRequest {

    @NotBlank(message = "제목은 필수 입력 항목입니다")
    @Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다")
    private String title;

    @Size(max = 100, message = "설명은 최대 100자까지 입력 가능합니다")
    private String description;

    @NotNull(message = "카테고리는 필수 입력 항목입니다")
    private Set<MatchingCategory> matchingCategories;

    private boolean isAnonymous;
    private boolean allowRandom;
    private boolean showDepartment;
}