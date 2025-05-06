package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentProductResponse {
    private Long id;
    private Integer points;
    private Integer amount;
    private Boolean isPromotion;
    private String promotionPeriod;

    public static PaymentProductResponse from(PaymentProduct product) {
        return PaymentProductResponse.builder()
                .id(product.getId())
                .points(product.getPoints())
                .amount(product.getAmount())
                .isPromotion(product.getIsPromotion())
                .promotionPeriod(product.getPromotionPeriod())
                .build();
    }
}