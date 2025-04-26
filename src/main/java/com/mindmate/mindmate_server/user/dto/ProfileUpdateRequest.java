package com.mindmate.mindmate_server.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileUpdateRequest {
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다")
    private String nickname;

    private String profileImage;

    @Size(max = 20, message = "학과명은 최대 20자까지 가능합니다")
    private String department;

    @Min(value = 1950, message = "입학년도는 1950년 이후여야 합니다")
    @Max(value = 2100, message = "유효하지 않은 입학년도입니다")
    private Integer entranceTime;

    private Boolean graduation;
}

