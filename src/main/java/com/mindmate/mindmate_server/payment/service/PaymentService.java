package com.mindmate.mindmate_server.payment.service;

import com.mindmate.mindmate_server.payment.dto.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentService {
    List<PaymentProductResponse> getProducts();

    PaymentOrderResponse createOrder(Long userId, Long productId);

    PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request);

    PaymentConfigResponse getPaymentConfig();
}
