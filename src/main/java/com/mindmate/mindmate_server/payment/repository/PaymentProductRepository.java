package com.mindmate.mindmate_server.payment.repository;

import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentProductRepository extends JpaRepository<PaymentProduct, Long> {
    List<PaymentProduct> findByActiveTrue();

    List<PaymentProduct> findByActiveFalse();
}