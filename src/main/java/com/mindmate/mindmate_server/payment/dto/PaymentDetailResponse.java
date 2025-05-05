package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import com.mindmate.mindmate_server.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentDetailResponse {
    private Long id;
    private String orderId;
    private String paymentKey;
    private Integer pointAmount;
    private Integer price;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String receiptUrl;

    public static PaymentDetailResponse from(PaymentOrder order, String receiptUrl) {
        return PaymentDetailResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .paymentKey(order.getPaymentKey())
                .pointAmount(order.getProduct().getPointAmount())
                .price(order.getPrice())
                .status(order.getStatus())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .receiptUrl(receiptUrl)
                .build();
    }
}