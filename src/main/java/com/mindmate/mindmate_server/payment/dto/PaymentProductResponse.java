package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentProductResponse {
    private Long id;
    private String name;
    private Integer amount;
    private Integer pointValue;
    private String description;
    private Boolean isPromotion;
    private String promotionPeriod;

    public static PaymentProductResponse fromEntity(PaymentProduct product) {
        return PaymentProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .amount(product.getAmount())
                .pointValue(product.getPointValue())
                .description(product.getDescription())
                .isPromotion(product.getIsPromotion())
                .promotionPeriod(product.getPromotionPeriod())
                .build();
    }
}