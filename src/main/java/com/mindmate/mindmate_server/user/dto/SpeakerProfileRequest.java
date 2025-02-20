package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpeakerProfileRequest {
    @NotBlank(message = "닉네임은 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_]{2,20}$", message = "닉네임은 2-20자의 영문, 한글, 숫자, 언더스코어만 가능합니다")
    private String nickname;

    private String profileImage;

    @NotNull(message = "선호하는 상담 스타일을 선택해주세요")
    private CounselingStyle preferredCounselingStyle;
}
