package com.mindmate.mindmate_server.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationProcessRequest {
    @NotNull(message = "승인 여부는 필수입니ㅏ")
    private Boolean isApproved;
    private String comment;  // 반려 또는 승인 시 코멘트
    private String badgeStatus;  // 승인 시 줄 배지
}