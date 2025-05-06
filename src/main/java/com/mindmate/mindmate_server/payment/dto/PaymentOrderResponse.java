package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponse {
    private String orderId;
    private Integer points;
    private Integer amount;

    public static PaymentOrderResponse from(PaymentOrder order) {
        return PaymentOrderResponse.builder()
                .orderId(order.getOrderId())
                .points(order.getProduct().getPoints())
                .amount(order.getAmount())
                .build();
    }
}
