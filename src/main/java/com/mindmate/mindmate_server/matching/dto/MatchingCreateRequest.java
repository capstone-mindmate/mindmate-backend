package com.mindmate.mindmate_server.matching.dto;

import com.mindmate.mindmate_server.matching.domain.InitiatorType;
import com.mindmate.mindmate_server.matching.domain.MatchingCategory;
import com.mindmate.mindmate_server.matching.domain.MatchingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 2, max = 100, message = "제목은 2~100자 이내로 입력해주세요")
    private String title;

    @Size(max = 500, message = "설명은 500자 이내로 입력해주세요")
    private String description;

    @NotNull(message = "고민 카테고리는 필수입니다")
    private MatchingCategory matchingCategory;

    @NotNull(message = "역할 유형은 필수입니다")
    private InitiatorType creatorRole;
}
