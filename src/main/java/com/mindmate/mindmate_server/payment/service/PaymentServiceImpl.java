package com.mindmate.mindmate_server.payment.service;

import com.mindmate.mindmate_server.global.config.TossPaymentsConfig;
import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import com.mindmate.mindmate_server.payment.domain.PaymentStatus;
import com.mindmate.mindmate_server.payment.dto.*;
import com.mindmate.mindmate_server.payment.repository.PaymentOrderRepository;
import com.mindmate.mindmate_server.payment.repository.PaymentProductRepository;
import com.mindmate.mindmate_server.point.dto.PointAddRequest;
import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    private final PaymentProductRepository paymentProductRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final UserService userService;
    private final PointService pointService;
    private final TossPaymentsConfig tossPaymentsConfig;
    private final RestTemplate restTemplate;

    @Override
    public List<PaymentProductResponse> getProducts() {
        List<PaymentProduct> products = paymentProductRepository.findByActiveTrue();
        return products.stream()
                .map(PaymentProductResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PaymentOrderResponse createOrder(Long userId, Long productId) {
        PaymentProduct product = paymentProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        User user = userService.findUserById(userId);

        String orderId = UUID.randomUUID().toString();

        PaymentOrder order = PaymentOrder.builder()
                .user(user)
                .product(product)
                .orderId(orderId)
                .price(product.getPrice())
                .status(PaymentStatus.READY)
                .build();

        paymentOrderRepository.save(order);

        return PaymentOrderResponse.builder()
                .orderId(orderId)
                .pointAmount(product.getPointAmount())
                .price(product.getPrice())
                .build();
    }

    @Override
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if (!order.getPrice().equals(request.getPrice())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossPaymentsConfig.getSecretKey(), "");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentKey", request.getPaymentKey());
        requestBody.put("orderId", request.getOrderId());
        requestBody.put("price", request.getPrice());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tossPaymentsConfig.getConfirmUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            order.completePayment(request.getPaymentKey());
            paymentOrderRepository.save(order);

            PointAddRequest pointRequest = PointAddRequest.builder()
                    .amount(order.getProduct().getPointAmount())
                    .reasonType(PointReasonType.PURCHASE)
                    .entityId(order.getId())
                    .build();

            PointTransactionResponse pointResponse = pointService.addPoints(order.getUser().getId(), pointRequest);

            return PaymentConfirmResponse.builder()
                    .orderId(order.getOrderId())
                    .status("success")
                    .paymentKey(order.getPaymentKey())
                    .price(order.getPrice())
                    .addedPoints(order.getProduct().getPointAmount())
                    .build();
        } catch (Exception e) {
            order.failPayment();
            paymentOrderRepository.save(order);
            throw new RuntimeException("결제 승인 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentConfigResponse getPaymentConfig() {
        return PaymentConfigResponse.builder()
                .clientKey(tossPaymentsConfig.getClientKey())
                .successCallbackUrl(tossPaymentsConfig.getSuccessCallbackUrl())
                .failCallbackUrl(tossPaymentsConfig.getFailCallbackUrl())
                .build();
    }
}