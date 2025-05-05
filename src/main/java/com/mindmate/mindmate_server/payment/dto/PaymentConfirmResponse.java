package com.mindmate.mindmate_server.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmResponse {
    private String orderId;
    private String status;
    private String paymentKey;
    private Integer price;
    private Integer addedPoints;
}