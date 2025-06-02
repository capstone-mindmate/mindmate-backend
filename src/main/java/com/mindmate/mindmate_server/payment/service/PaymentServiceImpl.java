package com.mindmate.mindmate_server.payment.service;

import com.mindmate.mindmate_server.global.config.TossPaymentsConfig;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.PaymentErrorCode;
import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import com.mindmate.mindmate_server.payment.domain.PaymentStatus;
import com.mindmate.mindmate_server.payment.dto.*;
import com.mindmate.mindmate_server.payment.repository.PaymentOrderRepository;
import com.mindmate.mindmate_server.payment.repository.PaymentProductRepository;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointRequest;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PRODUCT_NOT_FOUND));

        if (!product.getActive()) {
            throw new CustomException(PaymentErrorCode.INACTIVE_PRODUCT);
        }

        User user = userService.findUserById(userId);

        String orderId = UUID.randomUUID().toString();

        PaymentOrder order = PaymentOrder.builder()
                .user(user)
                .product(product)
                .orderId(orderId)
                .amount(product.getAmount())
                .status(PaymentStatus.READY)
                .build();

        paymentOrderRepository.save(order);

        return PaymentOrderResponse.builder()
                .orderId(orderId)
                .points(product.getPoints())
                .amount(product.getAmount())
                .build();
    }

    @Override
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        PaymentOrder order = paymentOrderRepository.findByOrderIdWithLock(request.getOrderId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.ORDER_NOT_FOUND));

        if (!order.getAmount().equals(request.getAmount())) {
            throw new CustomException(PaymentErrorCode.UNMATCHED_AMOUNT);
        }

        if (order.getStatus() == PaymentStatus.DONE) {
            return PaymentConfirmResponse.builder()
                    .orderId(order.getOrderId())
                    .status("success")
                    .paymentKey(order.getPaymentKey())
                    .amount(order.getAmount())
                    .addedPoints(order.getProduct().getPoints())
                    .build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(tossPaymentsConfig.getSecretKey(), "");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentKey", request.getPaymentKey());
        requestBody.put("orderId", request.getOrderId());
        requestBody.put("amount", request.getAmount());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    tossPaymentsConfig.getConfirmUrl(),
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
        } catch (Exception e) {
            order.failPayment();
            paymentOrderRepository.save(order);
            throw new CustomException(PaymentErrorCode.FAILED_CONFIRM_PAYMENT);
        }

        order.completePayment(request.getPaymentKey());
        paymentOrderRepository.save(order);

        PointRequest pointRequest = PointRequest.builder()
                .transactionType(TransactionType.EARN)
                .amount(order.getProduct().getPoints())
                .reasonType(PointReasonType.PURCHASE)
                .entityId(order.getId())
                .build();

        pointService.addPoints(order.getUser().getId(), pointRequest);

        return PaymentConfirmResponse.builder()
                .orderId(order.getOrderId())
                .status("success")
                .paymentKey(order.getPaymentKey())
                .amount(order.getAmount())
                .addedPoints(order.getProduct().getPoints())
                .build();
    }

    @Override
    public PaymentConfigResponse getPaymentConfig() {
        return PaymentConfigResponse.builder()
                .clientKey(tossPaymentsConfig.getClientKey())
                .successCallbackUrl(tossPaymentsConfig.getSuccessCallbackUrl())
                .failCallbackUrl(tossPaymentsConfig.getFailCallbackUrl())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> getUserPaymentHistory(Long userId, int page, int size) {
        User user = userService.findUserById(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentOrder> orderPage = paymentOrderRepository
                .findByUserIdWithProductOrderByCreatedAtDesc(userId, pageable);

        return orderPage.map(PaymentHistoryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetail(Long userId, String orderId) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(PaymentErrorCode.UNAUTHORIZED);
        }

        String receiptUrl = null;
        if (order.getStatus() == PaymentStatus.DONE && order.getPaymentKey() != null) {
            receiptUrl = "https://dashboard.tosspayments.com/receipt/" + order.getPaymentKey();
        }

        return PaymentDetailResponse.from(order, receiptUrl);
    }

    @Override
    public List<PaymentProductResponse> getAllProducts(Boolean active) {
        List<PaymentProduct> products;
        if (active != null) {
            products = active ? paymentProductRepository.findByActiveTrue() :
                    paymentProductRepository.findByActiveFalse();
        } else {
            products = paymentProductRepository.findAll();
        }
        return products.stream()
                .map(PaymentProductResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentProductResponse createProduct(PaymentProductRequest request) {
        validateProductRequest(request);

        PaymentProduct product = PaymentProduct.builder()
                .points(request.getPoints())
                .amount(request.getAmount())
                .isPromotion(request.getIsPromotion())
                .promotionPeriod(request.getPromotionPeriod())
                .build();

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        PaymentProduct savedProduct = paymentProductRepository.save(product);
        return PaymentProductResponse.from(savedProduct);
    }

    @Override
    @Transactional
    public PaymentProductResponse updateProduct(Long productId, PaymentProductRequest request) {
        PaymentProduct product = paymentProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PRODUCT_NOT_FOUND));

        validateProductRequest(request);

        product.update(
                request.getPoints(),
                request.getAmount(),
                request.getIsPromotion(),
                request.getPromotionPeriod(),
                request.getActive()
        );

        PaymentProduct updatedProduct = paymentProductRepository.save(product);
        return PaymentProductResponse.from(updatedProduct);
    }

    @Override
    @Transactional
    public PaymentProductResponse toggleProductStatus(Long productId) {
        PaymentProduct product = paymentProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PRODUCT_NOT_FOUND));

        product.toggleActive();

        PaymentProduct updatedProduct = paymentProductRepository.save(product);
        return PaymentProductResponse.from(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        PaymentProduct product = paymentProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PRODUCT_NOT_FOUND));
        paymentProductRepository.delete(product);
    }

    private void validateProductRequest(PaymentProductRequest request) {
        if (request.getPoints() == null || request.getPoints() <= 0) {
            throw new CustomException(PaymentErrorCode.INVALID_PRODUCT_POINTS);
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new CustomException(PaymentErrorCode.INVALID_PRODUCT_AMOUNT);
        }

        if (Boolean.TRUE.equals(request.getIsPromotion()) &&
                (request.getPromotionPeriod() == null || request.getPromotionPeriod().isEmpty())) {
            throw new CustomException(PaymentErrorCode.INVALID_PROMOTION_PERIOD);
        }
    }
}