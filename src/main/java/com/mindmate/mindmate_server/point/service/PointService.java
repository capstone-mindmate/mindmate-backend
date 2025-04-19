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

    PointTransaction addPoints(Long userId, int amount, PointReasonType reasonType, Long entityId);

    PointTransaction usePoints(Long userId, int amount, PointReasonType reasonType, Long entityId);

    int getCurrentBalance(Long userId);

    Page<PointTransaction> getTransactionHistory(Long userId, Pageable pageable);

    int getTotalEarnedPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    int getTotalSpentPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end);

    int getTotalEarnedPoints(Long userId);

    int getTotalSpentPoints(Long userId);

    PointSummaryResponse getUserPointSummary(Long userId);
}
