package com.mindmate.mindmate_server.point.repository;

import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.PointTransaction;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    Optional<PointTransaction> findTopByUserOrderByVersionDesc(User user);

    Page<PointTransaction> findByUserOrderByVersionDesc(User user, Pageable pageable);

    Page<PointTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<PointTransaction> findByUserAndTransactionType(User user, TransactionType transactionType);

    List<PointTransaction> findByUserAndCreatedAtBetweenAndTransactionType(
            User user, LocalDateTime start, LocalDateTime end, TransactionType transactionType);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PointTransaction p WHERE p.user.id = :userId AND p.transactionType = :type")
    Integer sumAmountByUserIdAndTransactionType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findUserByIdWithLock(@Param("userId") Long userId);
}