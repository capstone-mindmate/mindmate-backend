package com.mindmate.mindmate_server.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfigResponse {
    private String clientKey;
    private String successCallbackUrl;
    private String failCallbackUrl;
}
