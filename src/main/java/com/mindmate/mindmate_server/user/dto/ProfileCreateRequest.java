package com.mindmate.mindmate_server.user.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileCreateRequest {
    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다")
    private String nickname;

    private Long profileImageId;

    @NotBlank(message = "학과는 필수입니다")
    @Size(max = 20, message = "학과명은 최대 20자까지 가능합니다.")
    private String department;

    @NotNull(message = "입학년도는 필수입니다")
    @Min(value = 1950, message = "입학년도는 1950년 이후여야 합니다.")
    @Max(value = 2100, message = "유효하지 않은 입학년도입니다")
    private Integer entranceTime;

    private boolean graduation;

    @AssertTrue(message = "개인 정보 동의를 체크해주세요.")
    private boolean agreeToTerm;
}
