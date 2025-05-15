package com.mindmate.mindmate_server.payment.service;

import com.mindmate.mindmate_server.payment.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PaymentService {
    List<PaymentProductResponse> getProducts();

    PaymentOrderResponse createOrder(Long userId, Long productId);

    PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request);

    PaymentConfigResponse getPaymentConfig();

//    List<PaymentHistoryResponse> getUserPaymentHistory(Long userId);

    Page<PaymentHistoryResponse> getUserPaymentHistory(Long userId, int page, int size);

    PaymentDetailResponse getPaymentDetail(Long userId, String orderId);

    List<PaymentProductResponse> getAllProducts(Boolean active);

    PaymentProductResponse createProduct(PaymentProductRequest request);
    PaymentProductResponse updateProduct(Long productId, PaymentProductRequest request);
    PaymentProductResponse toggleProductStatus(Long productId);
    void deleteProduct(Long productId);
}
