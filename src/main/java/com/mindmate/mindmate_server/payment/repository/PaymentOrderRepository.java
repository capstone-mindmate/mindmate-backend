package com.mindmate.mindmate_server.payment.repository;

import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(String orderId);
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM PaymentOrder o JOIN FETCH o.product WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<PaymentOrder> findByUserIdWithProductOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query(value = "SELECT o FROM PaymentOrder o JOIN FETCH o.product WHERE o.user.id = :userId",
            countQuery = "SELECT COUNT(o) FROM PaymentOrder o WHERE o.user.id = :userId")
    Page<PaymentOrder> findByUserIdWithProductOrderByCreatedAtDesc(
            @Param("userId") Long userId, Pageable pageable);

    Optional<PaymentOrder> findByPaymentKey(String paymentKey);
}