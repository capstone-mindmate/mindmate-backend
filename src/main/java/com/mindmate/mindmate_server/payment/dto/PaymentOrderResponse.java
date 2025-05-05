package com.mindmate.mindmate_server.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderResponse {
    private String orderId;
    private String productName;
    private Integer amount;
}
