package com.mindmate.mindmate_server.user.dto;

import com.mindmate.mindmate_server.user.domain.CounselingField;
import com.mindmate.mindmate_server.user.domain.CounselingStyle;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class ListenerProfileRequest {
    @NotBlank(message = "닉네임은 필수입니다")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣_]{2,20}$", message = "닉네임은 2-20자의 영문, 한글, 숫자, 언더스코어만 가능합니다")
    private String nickname;

    private String profileImage;

    @NotEmpty(message = "상담 분야를 하나 이상 선택해주세요")
    @Size(max = 5, message = "상담 분야는 최대 5개까지 선택 가능합니다")
    private Set<CounselingField> counselingFields;

    @NotNull(message = "상담 스타일을 선택해주세요")
    private CounselingStyle counselingStyle;

    @NotNull(message = "상담 가능 시간을 설정해주세요")
    @Future(message = "상담 가능 시간은 현재 시간 이후여야 합니다")
    private LocalDateTime availableTime;
}
