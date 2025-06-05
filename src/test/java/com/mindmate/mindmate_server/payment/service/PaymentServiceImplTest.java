package com.mindmate.mindmate_server.payment.service;

import com.mindmate.mindmate_server.global.config.TossPaymentsConfig;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.PaymentErrorCode;
import com.mindmate.mindmate_server.global.exception.PointErrorCode;
import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import com.mindmate.mindmate_server.payment.domain.PaymentStatus;
import com.mindmate.mindmate_server.payment.dto.*;
import com.mindmate.mindmate_server.payment.repository.PaymentOrderRepository;
import com.mindmate.mindmate_server.payment.repository.PaymentProductRepository;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointRequest;
import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
import com.mindmate.mindmate_server.point.service.PointService;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PaymentServiceImplTest {

    @Mock private PaymentProductRepository paymentProductRepository;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private UserService userService;
    @Mock private PointService pointService;
    @Mock private TossPaymentsConfig tossPaymentsConfig;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private PaymentProduct testProduct;
    private PaymentOrder testOrder;
    private String orderId;
    private String paymentKey;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .role(RoleType.ROLE_PROFILE)
                .build();
        setField(testUser, "id", 1L);

        testProduct = PaymentProduct.builder()
                .points(1000)
                .amount(10000)
                .isPromotion(false)
                .build();
        setField(testProduct, "id", 1L);
        setField(testProduct, "active", true);

        orderId = UUID.randomUUID().toString();
        paymentKey = "paymentKey_" + UUID.randomUUID().toString();

        testOrder = PaymentOrder.builder()
                .user(testUser)
                .product(testProduct)
                .orderId(orderId)
                .amount(testProduct.getAmount())
                .status(PaymentStatus.READY)
                .build();
        setField(testOrder, "id", 1L);

        when(tossPaymentsConfig.getClientKey()).thenReturn("test_client_key");
        when(tossPaymentsConfig.getSecretKey()).thenReturn("test_secret_key");
        when(tossPaymentsConfig.getSuccessCallbackUrl()).thenReturn("https://example.com/success");
        when(tossPaymentsConfig.getFailCallbackUrl()).thenReturn("https://example.com/fail");
        when(tossPaymentsConfig.getConfirmUrl()).thenReturn("https://api.tosspayments.com/v1/payments/confirm");
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
            } catch (Exception ex) {
                throw new RuntimeException("Field setting failed: " + fieldName, ex);
            }
        }
    }
    @Test

    @DisplayName("활성화된 결제 상품 목록 조회")
    void getProducts() {
        when(paymentProductRepository.findByActiveTrue()).thenReturn(List.of(testProduct));

        List<PaymentProductResponse> result = paymentService.getProducts();

        assertEquals(1, result.size());
        assertEquals(testProduct.getId(), result.get(0).getProductId());
        verify(paymentProductRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("빈 상품 목록 조회")
    void getProductsEmptyList() {
        when(paymentProductRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        List<PaymentProductResponse> result = paymentService.getProducts();

        assertTrue(result.isEmpty());
        verify(paymentProductRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("모든 상품 조회 - 필터 없음")
    void getAllProductsWithoutFilter() {
        when(paymentProductRepository.findAll()).thenReturn(List.of(testProduct));

        List<PaymentProductResponse> result = paymentService.getAllProducts(null);

        assertEquals(1, result.size());
        verify(paymentProductRepository).findAll();
        verify(paymentProductRepository, never()).findByActiveTrue();
        verify(paymentProductRepository, never()).findByActiveFalse();
    }

    @Test
    @DisplayName("활성화된 상품만 조회")
    void getAllProductsActiveOnly() {
        when(paymentProductRepository.findByActiveTrue()).thenReturn(List.of(testProduct));

        List<PaymentProductResponse> result = paymentService.getAllProducts(true);

        assertEquals(1, result.size());
        verify(paymentProductRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("비활성화된 상품만 조회")
    void getAllProductsInactiveOnly() {
        PaymentProduct inactiveProduct = PaymentProduct.builder()
                .points(500)
                .amount(5000)
                .isPromotion(false)
                .build();
        setField(inactiveProduct, "active", false);

        when(paymentProductRepository.findByActiveFalse()).thenReturn(List.of(inactiveProduct));

        List<PaymentProductResponse> result = paymentService.getAllProducts(false);

        assertEquals(1, result.size());
        verify(paymentProductRepository).findByActiveFalse();
    }

    @Test
    @DisplayName("상품 생성 - 일반 상품")
    void createProduct() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(2000)
                .amount(20000)
                .isPromotion(false)
                .build();

        when(paymentProductRepository.save(any(PaymentProduct.class))).thenReturn(testProduct);

        PaymentProductResponse result = paymentService.createProduct(request);

        assertNotNull(result);
        verify(paymentProductRepository).save(any(PaymentProduct.class));
    }

    @Test
    @DisplayName("상품 생성 - 프로모션 상품")
    void createPromotionProduct() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(1500)
                .amount(10000)
                .isPromotion(true)
                .promotionPeriod("2024-06-01 ~ 2024-12-31")
                .active(true)
                .build();

        when(paymentProductRepository.save(any(PaymentProduct.class))).thenReturn(testProduct);

        PaymentProductResponse result = paymentService.createProduct(request);

        assertNotNull(result);
        verify(paymentProductRepository).save(any(PaymentProduct.class));
    }

    @Test
    @DisplayName("상품 생성 - 잘못된 포인트 값 (0)")
    void createProductWithZeroPoints() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(0)
                .amount(10000)
                .isPromotion(false)
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PRODUCT_POINTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 - 음수 포인트")
    void createProductWithNegativePoints() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(-100)
                .amount(10000)
                .isPromotion(false)
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PRODUCT_POINTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 - null 포인트")
    void createProductWithNullPoints() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(null)
                .amount(10000)
                .isPromotion(false)
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PRODUCT_POINTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 - 잘못된 금액 값")
    void createProductWithInvalidAmount() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(1000)
                .amount(0)
                .isPromotion(false)
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PRODUCT_AMOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 - null 금액")
    void createProductWithNullAmount() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(1000)
                .amount(null)
                .isPromotion(false)
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PRODUCT_AMOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 - 프로모션 기간 누락")
    void createProductWithMissingPromotionPeriod() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(1000)
                .amount(10000)
                .isPromotion(true)
                .promotionPeriod(null)
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PROMOTION_PERIOD, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 생성 - 빈 프로모션 기간")
    void createProductWithEmptyPromotionPeriod() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(1000)
                .amount(10000)
                .isPromotion(true)
                .promotionPeriod("")
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createProduct(request));

        assertEquals(PaymentErrorCode.INVALID_PROMOTION_PERIOD, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 업데이트")
    void updateProduct() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(2000)
                .amount(25000)
                .isPromotion(false)
                .build();

        when(paymentProductRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(paymentProductRepository.save(any(PaymentProduct.class))).thenReturn(testProduct);

        PaymentProductResponse result = paymentService.updateProduct(1L, request);

        assertNotNull(result);
        verify(paymentProductRepository).save(testProduct);
    }

    @Test
    @DisplayName("상품 업데이트 - 존재하지 않는 상품")
    void updateProductNotFound() {
        PaymentProductRequest request = PaymentProductRequest.builder()
                .points(1000)
                .amount(10000)
                .isPromotion(false)
                .build();

        when(paymentProductRepository.findById(999L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.updateProduct(999L, request));

        assertEquals(PaymentErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 상태 토글")
    void toggleProductStatus() {
        when(paymentProductRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(paymentProductRepository.save(any(PaymentProduct.class))).thenReturn(testProduct);

        PaymentProductResponse result = paymentService.toggleProductStatus(1L);

        assertNotNull(result);
        verify(paymentProductRepository).save(testProduct);
    }

    @Test
    @DisplayName("상품 상태 토글 - 존재하지 않는 상품")
    void toggleProductStatusNotFound() {
        when(paymentProductRepository.findById(999L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.toggleProductStatus(999L));

        assertEquals(PaymentErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 삭제")
    void deleteProduct() {
        when(paymentProductRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        paymentService.deleteProduct(1L);

        verify(paymentProductRepository).delete(testProduct);
    }

    @Test
    @DisplayName("상품 삭제 - 존재하지 않는 상품")
    void deleteProductNotFound() {
        when(paymentProductRepository.findById(999L)).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.deleteProduct(999L));

        assertEquals(PaymentErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 생성 - 정상 케이스")
    void createOrder() {
        when(paymentProductRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
        when(userService.findUserById(anyLong())).thenReturn(testUser);
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);

        PaymentOrderResponse result = paymentService.createOrder(testUser.getId(), testProduct.getId());

        assertNotNull(result.getOrderId());
        assertEquals(testProduct.getPoints(), result.getPoints());
        verify(paymentOrderRepository).save(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("주문 생성 - 존재하지 않는 상품")
    void createOrderWithNonExistingProduct() {
        when(paymentProductRepository.findById(anyLong())).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createOrder(testUser.getId(), 999L));

        assertEquals(PaymentErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("주문 생성 - 비활성화된 상품")
    void createOrderWithInactiveProduct() {
        PaymentProduct inactiveProduct = PaymentProduct.builder()
                .points(1000)
                .amount(10000)
                .isPromotion(false)
                .build();
        setField(inactiveProduct, "active", false);

        when(paymentProductRepository.findById(anyLong())).thenReturn(Optional.of(inactiveProduct));

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.createOrder(testUser.getId(), 1L));

        assertEquals(PaymentErrorCode.INACTIVE_PRODUCT, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 확인 - 정상 케이스")
    void confirmPayment() {
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(testProduct.getAmount())
                .build();

        when(paymentOrderRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(testOrder));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));

        PointTransactionResponse pointResponse = PointTransactionResponse.builder()
                .id(1L)
                .transactionType(TransactionType.EARN)
                .amount(testProduct.getPoints())
                .reasonType(PointReasonType.PURCHASE.getTitle())
                .entityId(testOrder.getId())
                .balance(testProduct.getPoints())
                .createdAt(LocalDateTime.now())
                .build();

        when(pointService.addPoints(eq(testUser.getId()), any(PointRequest.class))).thenReturn(pointResponse);

        PaymentConfirmResponse result = paymentService.confirmPayment(request);

        assertEquals(orderId, result.getOrderId());
        assertEquals("success", result.getStatus());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(pointService).addPoints(eq(testUser.getId()), any(PointRequest.class));
    }

    @Test
    @DisplayName("결제 확인 - 존재하지 않는 주문")
    void confirmPaymentWithNonExistingOrder() {
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId("non_existing")
                .amount(testProduct.getAmount())
                .build();

        when(paymentOrderRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(request));

        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 확인 - 금액 불일치")
    void confirmPaymentWithUnmatchedAmount() {
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(9999)
                .build();

        when(paymentOrderRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(testOrder));

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(request));

        assertEquals(PaymentErrorCode.UNMATCHED_AMOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 확인 - 이미 완료된 주문")
    void confirmPaymentWithAlreadyProcessedOrder() {
        PaymentOrder completedOrder = PaymentOrder.builder()
                .user(testUser)
                .product(testProduct)
                .orderId(orderId)
                .amount(testProduct.getAmount())
                .status(PaymentStatus.DONE)
                .build();
        setField(completedOrder, "paymentKey", paymentKey);

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(testProduct.getAmount())
                .build();

        when(paymentOrderRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(completedOrder));

        PaymentConfirmResponse result = paymentService.confirmPayment(request);

        assertEquals("success", result.getStatus());
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("결제 확인 - API 호출 실패")
    void confirmPaymentWithAPIFailure() {
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(testProduct.getAmount())
                .build();

        when(paymentOrderRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(testOrder));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API 호출 실패"));

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(request));

        assertEquals(PaymentErrorCode.FAILED_CONFIRM_PAYMENT, exception.getErrorCode());
        assertEquals(PaymentStatus.FAILED, testOrder.getStatus());
    }

    @Test
    @DisplayName("포인트 적립 중 예외 발생")
    void confirmPaymentWithPointServiceException() {
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(testProduct.getAmount())
                .build();

        when(paymentOrderRepository.findByOrderIdWithLock(anyString())).thenReturn(Optional.of(testOrder));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));
        when(pointService.addPoints(eq(testUser.getId()), any(PointRequest.class)))
                .thenThrow(new CustomException(PointErrorCode.TRANSACTION_CONFLICT));

        assertThrows(CustomException.class, () -> paymentService.confirmPayment(request));

        assertEquals(PaymentStatus.DONE, testOrder.getStatus());
    }

    @Test
    @DisplayName("사용자 결제 내역 조회")
    void getUserPaymentHistory() {
        List<PaymentOrder> orders = List.of(testOrder);
        Page<PaymentOrder> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 1);

        when(userService.findUserById(testUser.getId())).thenReturn(testUser);
        when(paymentOrderRepository.findByUserIdWithProductOrderByCreatedAtDesc(eq(testUser.getId()), any(Pageable.class)))
                .thenReturn(orderPage);

        Page<PaymentHistoryResponse> result = paymentService.getUserPaymentHistory(testUser.getId(), 0, 10);

        assertEquals(1, result.getContent().size());
        verify(paymentOrderRepository).findByUserIdWithProductOrderByCreatedAtDesc(eq(testUser.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("결제 내역 조회 - 빈 페이지")
    void getUserPaymentHistoryEmptyPage() {
        Page<PaymentOrder> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        when(userService.findUserById(testUser.getId())).thenReturn(testUser);
        when(paymentOrderRepository.findByUserIdWithProductOrderByCreatedAtDesc(eq(testUser.getId()), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<PaymentHistoryResponse> result = paymentService.getUserPaymentHistory(testUser.getId(), 0, 10);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("결제 상세 정보 조회 - DONE 상태")
    void getPaymentDetail() {
        testOrder.completePayment(paymentKey);
        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));

        PaymentDetailResponse result = paymentService.getPaymentDetail(testUser.getId(), orderId);

        assertEquals(testOrder.getId(), result.getId());
        assertTrue(result.getReceiptUrl().contains(paymentKey));
        verify(paymentOrderRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("결제 상세 정보 조회 - READY 상태 (영수증 URL 없음)")
    void getPaymentDetailWithReadyStatus() {
        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));

        PaymentDetailResponse result = paymentService.getPaymentDetail(testUser.getId(), orderId);

        assertEquals(PaymentStatus.READY, result.getStatus());
        assertNull(result.getReceiptUrl());
    }

    @Test
    @DisplayName("결제 상세 정보 조회 - 존재하지 않는 주문")
    void getPaymentDetailWithNonExistingOrder() {
        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.getPaymentDetail(testUser.getId(), "non_existing"));

        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 상세 정보 조회 - 권한 없음")
    void getPaymentDetailWithUnauthorizedUser() {
        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));

        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.getPaymentDetail(999L, orderId));

        assertEquals(PaymentErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 설정 정보 조회")
    void getPaymentConfig() {
        PaymentConfigResponse result = paymentService.getPaymentConfig();

        assertEquals("test_client_key", result.getClientKey());
        assertEquals("https://example.com/success", result.getSuccessCallbackUrl());
        assertEquals("https://example.com/fail", result.getFailCallbackUrl());
    }
}