package com.mindmate.mindmate_server.point.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.PointErrorCode;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.point.domain.PointTransaction;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.*;
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
    public PointTransactionResponse addPoints(Long userId, PointRequest request) {
        if(request.getTransactionType() != TransactionType.EARN) {
            throw new CustomException(PointErrorCode.INVALID_TRANSACTION_TYPE);
        }

        validateAmount(request.getAmount());
        return PointTransactionResponse.from(
                executeWithRetry(() -> createTransaction(userId, TransactionType.EARN, request))
        );
    }

    @Override
    public PointTransactionResponse usePoints(Long userId, PointRequest request) {
        if(request.getTransactionType() != TransactionType.SPEND) {
            throw new CustomException(PointErrorCode.INVALID_TRANSACTION_TYPE);
        }

        validateAmount(request.getAmount());
        ensureSufficientBalance(userId, request.getAmount());
        return PointTransactionResponse.from(
                executeWithRetry(() -> createTransaction(userId, TransactionType.SPEND, request))
        );
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
    public Page<PointTransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        User user = userService.findUserById(userId);
        return pointTransactionRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(PointTransactionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalEarnedPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        return getTotalPointsInPeriod(userId, start, end, TransactionType.EARN);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalSpentPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end) {
        return getTotalPointsInPeriod(userId, start, end, TransactionType.SPEND);
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
        return PointSummaryResponse.builder()
                .currentBalance(getCurrentBalance(userId))
                .totalEarned(getTotalEarnedPoints(userId))
                .totalSpent(getTotalSpentPoints(userId))
                .build();
    }

    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new CustomException(PointErrorCode.INVALID_POINT_AMOUNT);
        }
    }

    private void ensureSufficientBalance(Long userId, int amount) {
        if (getCurrentBalance(userId) < amount) {
            throw new CustomException(PointErrorCode.INSUFFICIENT_POINTS);
        }
    }

    private int getTotalPointsInPeriod(Long userId, LocalDateTime start, LocalDateTime end, TransactionType type) {
        User user = userService.findUserById(userId);
        return pointTransactionRepository.findByUserAndCreatedAtBetweenAndTransactionType(user, start, end, type)
                .stream()
                .mapToInt(PointTransaction::getAmount)
                .sum();
    }

    private PointTransaction createTransaction(Long userId, TransactionType type, PointRequest request) {
        User user = pointTransactionRepository.findUserByIdWithLock(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        PointTransaction lastTx = pointTransactionRepository.findTopByUserOrderByVersionDesc(user).orElse(null);

        Long currentVersion = (lastTx != null) ? lastTx.getVersion() : 0L;
        int currentBalance = (lastTx != null) ? lastTx.getBalance() : 0;

        int newBalance = (type == TransactionType.EARN)
                ? currentBalance + request.getAmount()
                : currentBalance - request.getAmount();

        if (type == TransactionType.SPEND && newBalance < 0) {
            throw new CustomException(PointErrorCode.INSUFFICIENT_POINTS);
        }

        PointTransaction newTransaction = PointTransaction.builder()
                .user(user)
                .transactionType(type)
                .amount(request.getAmount())
                .reasonType(request.getReasonType())
                .entityId(request.getEntityId())
                .balance(newBalance)
                .build();

        newTransaction.incrementVersion(currentVersion);

        return pointTransactionRepository.save(newTransaction);

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
                    Thread.sleep(retryDelayMs * (long) Math.pow(2, attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new CustomException(PointErrorCode.UNEXPECTED_ERROR);
    }
}
