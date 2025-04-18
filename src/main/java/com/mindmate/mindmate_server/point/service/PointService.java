package com.mindmate.mindmate_server.point.service;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.dto.PointAddRequest;
import com.mindmate.mindmate_server.point.dto.PointBalanceResponse;
import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
import com.mindmate.mindmate_server.point.dto.PointUseRequest;
import com.mindmate.mindmate_server.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface PointService {
    @Transactional
    PointBalanceResponse addPoints(User user, int amount, PointReasonType reason);

    @Transactional
    PointBalanceResponse usePoints(User user, PointUseRequest request);

    @Transactional
    PointBalanceResponse addPointsByAdmin(Long userId, PointAddRequest request);

    @Transactional(readOnly = true)
    PointBalanceResponse getBalance(User user);

    @Transactional(readOnly = true)
    Page<PointTransactionResponse> getTransactionHistory(User user, Pageable pageable);
}
