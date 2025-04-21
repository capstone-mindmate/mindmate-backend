package com.mindmate.mindmate_server.point.service;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.PointTransaction;
import com.mindmate.mindmate_server.point.dto.*;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface PointService {

    PointTransactionResponse addPoints(Long userId, PointAddRequest request);

    PointTransactionResponse usePoints(Long userId, PointUseRequest request);

    int getCurrentBalance(Long userId);

    Page<PointTransactionResponse> getTransactionHistory(Long userId, Pageable pageable);

    int getTotalEarnedPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    int getTotalSpentPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    int getTotalEarnedPoints(Long userId);

    int getTotalSpentPoints(Long userId);

    PointSummaryResponse getUserPointSummary(Long userId);
}
