package com.mindmate.mindmate_server.payment.dto;

import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import com.mindmate.mindmate_server.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentHistoryResponse {
    private Long id;
    private String orderId;
    private Integer points;
    private Integer amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static PaymentHistoryResponse from(PaymentOrder order) {
        return PaymentHistoryResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .points(order.getProduct().getPoints())
                .amount(order.getAmount())
                .status(order.getStatus())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}