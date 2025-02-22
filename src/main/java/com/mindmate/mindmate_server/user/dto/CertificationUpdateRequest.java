package com.mindmate.mindmate_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationUpdateRequest {

    @NotBlank(message = "증명서 URL은 필수입니다.")
    private String certificationUrl;

    @NotBlank(message = "경력 기재는 필수입니다.")
    private String careerDescription;
}
