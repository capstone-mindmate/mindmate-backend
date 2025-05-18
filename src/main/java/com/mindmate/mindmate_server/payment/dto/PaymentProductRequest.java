package com.mindmate.mindmate_server.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentProductRequest {
    private Integer points;
    private Integer amount;
    private Boolean isPromotion;
    private String promotionPeriod;
    private Boolean active;
}
