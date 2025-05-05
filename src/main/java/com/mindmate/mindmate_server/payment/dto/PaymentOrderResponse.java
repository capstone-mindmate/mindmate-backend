package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponse {
    private String orderId;
    private Integer pointAmount;
    private Integer price;

    public static PaymentOrderResponse from(PaymentOrder order) {
        return PaymentOrderResponse.builder()
                .orderId(order.getOrderId())
                .pointAmount(order.getProduct().getPointAmount())
                .price(order.getPrice())
                .build();
    }
}
