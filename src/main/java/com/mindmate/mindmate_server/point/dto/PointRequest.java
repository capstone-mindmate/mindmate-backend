package com.mindmate.mindmate_server.point.dto;

import com.mindmate.mindmate_server.point.domain.PointReasonType;

public interface PointRequest {
    int getAmount();
    PointReasonType getReasonType();
    Long getEntityId();
}
