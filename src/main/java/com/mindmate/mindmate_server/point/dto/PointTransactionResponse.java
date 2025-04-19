package com.mindmate.mindmate_server.point.dto;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.PointTransaction;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointTransactionResponse {
    private Long id;
    private TransactionType transactionType;
    private int amount;
    private PointReasonType reasonType;
    private Long entityId;
    private int balance;
    private LocalDateTime createdAt;

    public static PointTransactionResponse from(PointTransaction transaction) {
        return PointTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .reasonType(transaction.getReasonType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}