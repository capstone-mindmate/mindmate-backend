package com.mindmate.mindmate_server.point.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.PointErrorCode;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.PointTransaction;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointSummaryResponse;
import com.mindmate.mindmate_server.point.repository.PointTransactionRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional
public class PointServiceImpl implements PointService {
    private final PointTransactionRepository pointTransactionRepository;
    private final UserService userService;

    @Override
    public PointTransaction addPoints(Long userId, int amount, PointReasonType reasonType, Long entityId) {
        if (amount <= 0) {
            throw new CustomException(PointErrorCode.INVALID_POINT_AMOUNT);
        }

        return executeWithRetry(() -> createTransaction(userId, TransactionType.EARN, amount, reasonType, entityId));
    }

    @Override
    public PointTransaction usePoints(Long userId, int amount, PointReasonType reasonType, Long entityId) {
        if (amount <= 0) {
            throw new CustomException(PointErrorCode.INVALID_POINT_AMOUNT);
        }

        int balance = getCurrentBalance(userId);
        if (balance < amount) {
            throw new CustomException(PointErrorCode.INSUFFICIENT_POINTS);
        }

        return executeWithRetry(() -> createTransaction(userId, TransactionType.SPEND, amount, reasonType, entityId));
    }

    @Override
    @Transactional(readOnly = true)
    public int getCurrentBalance(Long userId) {
        User user = userService.findUserById(userId);
        return pointTransactionRepository.findTopByUserOrderByVersionDesc(user)
                .map(PointTransaction::getBalance)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        User user = userService.findUserById(userId);
        return pointTransactionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalEarnedPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        User user = userService.findUserById(userId);
        return pointTransactionRepository.findByUserAndCreatedAtBetweenAndTransactionType(
                        user, start, end, TransactionType.EARN)
                .stream()
                .mapToInt(PointTransaction::getAmount)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalSpentPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        User user = userService.findUserById(userId);
        return pointTransactionRepository.findByUserAndCreatedAtBetweenAndTransactionType(
                        user, start, end, TransactionType.SPEND)
                .stream()
                .mapToInt(PointTransaction::getAmount)
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalEarnedPoints(Long userId) {
        return pointTransactionRepository.sumAmountByUserIdAndTransactionType(userId, TransactionType.EARN);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalSpentPoints(Long userId) {
        return pointTransactionRepository.sumAmountByUserIdAndTransactionType(userId, TransactionType.SPEND);
    }

    @Override
    @Transactional(readOnly = true)
    public PointSummaryResponse getUserPointSummary(Long userId) {
        int currentBalance = getCurrentBalance(userId);

        int totalEarned = getTotalEarnedPoints(userId);
        int totalSpent = getTotalSpentPoints(userId);

        return PointSummaryResponse.builder()
                .currentBalance(currentBalance)
                .totalEarned(totalEarned)
                .totalSpent(totalSpent)
                .build();
    }


    private PointTransaction createTransaction(
            Long userId,
            TransactionType transactionType,
            int amount,
            PointReasonType reasonType,
            Long entityId) {
        User user = userService.findUserById(userId);

        PointTransaction lastTx = pointTransactionRepository
                .findTopByUserOrderByVersionDesc(user)
                .orElse(null);

        long newVersion = lastTx != null ? lastTx.getVersion() + 1 : 1;
        int currentBalance = lastTx != null ? lastTx.getBalance() : 0;

        int newBalance;
        if (transactionType == TransactionType.EARN) {
            newBalance = currentBalance + amount;
        } else {
            newBalance = currentBalance - amount;
        }

        PointTransaction newTx = PointTransaction.builder()
                .user(user)
                .version(newVersion)
                .transactionType(transactionType)
                .amount(amount)
                .reasonType(reasonType)
                .entityId(entityId)
                .balance(newBalance)
                .build();

        return pointTransactionRepository.save(newTx);
    }

    private PointTransaction executeWithRetry(Supplier<PointTransaction> operation) {
        int maxRetries = 3;
        int retryDelayMs = 50;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (OptimisticLockingFailureException e) {
                if (attempt == maxRetries - 1) {
                    throw new CustomException(PointErrorCode.TRANSACTION_CONFLICT);
                }
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new CustomException(PointErrorCode.UNEXPECTED_ERROR);
    }
}