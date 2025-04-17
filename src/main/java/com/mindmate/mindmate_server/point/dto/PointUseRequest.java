package com.mindmate.mindmate_server.point.dto;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PointUseRequest {
    @NotNull(message = "포인트 사용 금액은 필수입니다")
    @Min(value = 1, message = "포인트는 최소 1점 이상 사용해야 합니다.")
    private Integer amount;

    @NotNull(message = "포인트 사용 사유는 필수입니다.")
    private PointReasonType reason;
}