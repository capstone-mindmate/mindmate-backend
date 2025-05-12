package com.mindmate.mindmate_server.point.dto;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointRequest {
    private TransactionType transactionType;
    private int amount;
    private PointReasonType reasonType;
    private Long entityId;

    public static PointRequest forAddPoints(int amount, PointReasonType reason, Long entityId) {
        return PointRequest.builder()
                .transactionType(TransactionType.EARN)
                .amount(amount)
                .reasonType(reason)
                .entityId(entityId)
                .build();
    }

    public static PointRequest forUsePoints(int amount, PointReasonType reason, Long entityId) {
        return PointRequest.builder()
                .transactionType(TransactionType.SPEND)
                .amount(amount)
                .reasonType(reason)
                .entityId(entityId)
                .build();
    }
}