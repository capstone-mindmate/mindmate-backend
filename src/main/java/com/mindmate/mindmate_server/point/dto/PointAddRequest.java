package com.mindmate.mindmate_server.point.dto;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PointAddRequest {
    // 이건 관리자가 한다면?

    @NotNull(message = "포인트 금액은 필수입니다")
    @Min(value = 1, message = "포인트는 최소 1점 이상으로 설정해수세요.")
    private Integer amount;

    @NotNull(message = "포인트 적립 사유 작성은 필수입니다.")
    private PointReasonType reason;
}