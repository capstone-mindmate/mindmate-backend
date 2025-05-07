package com.mindmate.mindmate_server.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentFailResponse {
    private String orderId;
    private String errorCode;
    private String errorMessage;
}