package com.mindmate.mindmate_server.point.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.PointErrorCode;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.PointTransaction;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointAddRequest;
import com.mindmate.mindmate_server.point.dto.PointBalanceResponse;
import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
import com.mindmate.mindmate_server.point.dto.PointUseRequest;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointServiceImpl implements PointService {
    private final UserService userService;
    private final PointTransactionRepository pointTransactionRepository;
    private final PointBalanceRepository pointBalanceRepository;

    @Override
    @Transactional
    public PointBalanceResponse addPoints(User user, int amount, PointReasonType reason) {
        validateAmount(amount);

        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .amount(amount)
                .transactionType(TransactionType.EARN)
                .reason(reason)
                .build();
        pointTransactionRepository.save(transaction);

        PointBalance balance = getOrCreatePointBalance(user);
        balance.addPoints(amount);
        pointBalanceRepository.save(balance);

        return PointBalanceResponse.builder()
                .userId(user.getId())
                .balance(balance.getBalance())
                .build();
    }

    @Override
    @Transactional
    public PointBalanceResponse usePoints(User user, PointUseRequest request) {
        int amount = request.getAmount();
        validateAmount(amount);

        PointBalance balance = getOrCreatePointBalance(user);
        if (!balance.usePoints(amount)) {
            throw new CustomException(PointErrorCode.INSUFFICIENT_POINTS);
        }

        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .amount(-amount)
                .transactionType(TransactionType.SPEND)
                .reason(request.getReason())
                .build();
        pointTransactionRepository.save(transaction);
        pointBalanceRepository.save(balance);

        return PointBalanceResponse.builder()
                .userId(user.getId())
                .balance(balance.getBalance())
                .build();
    }

    @Override
    @Transactional
    public PointBalanceResponse addPointsByAdmin(Long userId, PointAddRequest request) {
        User user = userService.findUserById(userId);
        return addPoints(user, request.getAmount(), request.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(User user) {
        int balance = pointBalanceRepository.findByUserId(user.getId())
                .map(PointBalance::getBalance)
                .orElse(0);

        return PointBalanceResponse.builder()
                .userId(user.getId())
                .balance(balance)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> getTransactionHistory(User user, Pageable pageable) {
        return pointTransactionRepository
                .findByUserOrderByCreatedAtDesc(user, pageable)
                .map(PointTransactionResponse::from);
    }

    private PointBalance getOrCreatePointBalance(User user) {
        return pointBalanceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    PointBalance newBalance = new PointBalance(user);
                    return pointBalanceRepository.save(newBalance);
                });
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new CustomException(PointErrorCode.INVALID_POINT_AMOUNT);
        }
    }
}
