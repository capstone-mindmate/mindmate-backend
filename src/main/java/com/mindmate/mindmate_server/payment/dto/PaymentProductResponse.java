package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentProductResponse {
    private Long id;
    private Integer pointAmount;
    private Integer price;
    private Boolean isPromotion;
    private String promotionPeriod;

    public static PaymentProductResponse from(PaymentProduct product) {
        return PaymentProductResponse.builder()
                .id(product.getId())
                .pointAmount(product.getPointAmount())
                .price(product.getPrice())
                .isPromotion(product.getIsPromotion())
                .promotionPeriod(product.getPromotionPeriod())
                .build();
    }
}