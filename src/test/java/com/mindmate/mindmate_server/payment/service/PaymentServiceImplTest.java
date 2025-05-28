//package com.mindmate.mindmate_server.payment.service;
//
//import com.mindmate.mindmate_server.global.config.TossPaymentsConfig;
//import com.mindmate.mindmate_server.global.exception.CustomException;
//import com.mindmate.mindmate_server.global.exception.PaymentErrorCode;
//import com.mindmate.mindmate_server.global.exception.PointErrorCode;
//import com.mindmate.mindmate_server.payment.domain.PaymentOrder;
//import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
//import com.mindmate.mindmate_server.payment.domain.PaymentStatus;
//import com.mindmate.mindmate_server.payment.dto.*;
//import com.mindmate.mindmate_server.payment.repository.PaymentOrderRepository;
//import com.mindmate.mindmate_server.payment.repository.PaymentProductRepository;
//import com.mindmate.mindmate_server.point.domain.PointReasonType;
//import com.mindmate.mindmate_server.point.domain.TransactionType;
//import com.mindmate.mindmate_server.point.dto.PointAddRequest;
//import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
//import com.mindmate.mindmate_server.point.service.PointService;
//import com.mindmate.mindmate_server.user.domain.User;
//import com.mindmate.mindmate_server.user.domain.Profile;
//import com.mindmate.mindmate_server.user.domain.RoleType;
//import com.mindmate.mindmate_server.user.service.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//import org.springframework.data.domain.*;
//import org.springframework.http.*;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//public class PaymentServiceImplTest {
//
//    @Mock
//    private PaymentProductRepository paymentProductRepository;
//
//    @Mock
//    private PaymentOrderRepository paymentOrderRepository;
//
//    @Mock
//    private UserService userService;
//
//    @Mock
//    private PointService pointService;
//
//    @Mock
//    private TossPaymentsConfig tossPaymentsConfig;
//
//    @Mock
//    private RestTemplate restTemplate;
//
//    @InjectMocks
//    private PaymentServiceImpl paymentService;
//
//    private User testUser;
//    private PaymentProduct testProduct;
//    private PaymentOrder testOrder;
//    private String orderId;
//    private String paymentKey;
//
//    @BeforeEach
//    void setUp() {
//        testUser = User.builder()
//                .email("test@example.com")
//                .password("password123")
//                .agreedToTerms(true)
//                .role(RoleType.ROLE_PROFILE)
//                .build();
//        try {
//            java.lang.reflect.Field idField = testUser.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(testUser, 1L);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Profile testProfile = Profile.builder()
//                .user(testUser)
//                .nickname("테스트유저")
//                .department("컴퓨터공학과")
//                .entranceTime(2020)
//                .graduation(false)
//                .profileImage("default_profile.jpg")
//                .build();
//
//        try {
//            java.lang.reflect.Field idField = testProfile.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(testProfile, 1L);
//
//            java.lang.reflect.Field profileField = testUser.getClass().getDeclaredField("profile");
//            profileField.setAccessible(true);
//            profileField.set(testUser, testProfile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        testProduct = PaymentProduct.builder()
//                .points(1000)
//                .amount(10000)
//                .isPromotion(false)
//                .build();
//
//        try {
//            java.lang.reflect.Field idField = testProduct.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(testProduct, 1L);
//
//            java.lang.reflect.Field activeField = testProduct.getClass().getDeclaredField("active");
//            activeField.setAccessible(true);
//            activeField.set(testProduct, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        orderId = UUID.randomUUID().toString();
//        paymentKey = "paymentKey_" + UUID.randomUUID().toString();
//
//        testOrder = PaymentOrder.builder()
//                .user(testUser)
//                .product(testProduct)
//                .orderId(orderId)
//                .amount(testProduct.getAmount())
//                .status(PaymentStatus.READY)
//                .build();
//
//        try {
//            java.lang.reflect.Field idField = testOrder.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(testOrder, 1L);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        when(tossPaymentsConfig.getClientKey()).thenReturn("test_client_key");
//        when(tossPaymentsConfig.getSecretKey()).thenReturn("test_secret_key");
//        when(tossPaymentsConfig.getSuccessCallbackUrl()).thenReturn("https://example.com/success");
//        when(tossPaymentsConfig.getFailCallbackUrl()).thenReturn("https://example.com/fail");
//        when(tossPaymentsConfig.getConfirmUrl()).thenReturn("https://api.tosspayments.com/v1/payments/confirm");
//    }
//
//    @Test
//    @DisplayName("활성화된 결제 상품 목록 조회")
//    void getProducts() {
//        // given
//        List<PaymentProduct> mockProducts = List.of(testProduct);
//        when(paymentProductRepository.findByActiveTrue()).thenReturn(mockProducts);
//
//        // when
//        List<PaymentProductResponse> result = paymentService.getProducts();
//
//        // then
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(testProduct.getId(), result.get(0).getId());
//        assertEquals(testProduct.getPoints(), result.get(0).getPoints());
//        assertEquals(testProduct.getAmount(), result.get(0).getAmount());
//        assertEquals(testProduct.getIsPromotion(), result.get(0).getIsPromotion());
//
//        verify(paymentProductRepository, times(1)).findByActiveTrue();
//    }
//
//    @Test
//    @DisplayName("주문 생성")
//    void createOrder() {
//        // given
//        when(paymentProductRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
//        when(userService.findUserById(anyLong())).thenReturn(testUser);
//        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
//
//        // when
//        PaymentOrderResponse result = paymentService.createOrder(testUser.getId(), testProduct.getId());
//
//        // then
//        assertNotNull(result);
//        assertNotNull(result.getOrderId());
//        assertEquals(testProduct.getPoints(), result.getPoints());
//        assertEquals(testProduct.getAmount(), result.getAmount());
//
//        verify(paymentProductRepository, times(1)).findById(testProduct.getId());
//        verify(userService, times(1)).findUserById(testUser.getId());
//        verify(paymentOrderRepository, times(1)).save(any(PaymentOrder.class));
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 상품으로 주문 생성 시 예외 발생")
//    void createOrderWithNonExistingProduct() {
//        // given
//        when(paymentProductRepository.findById(anyLong())).thenReturn(Optional.empty());
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.createOrder(testUser.getId(), 999L);
//        });
//
//        assertEquals(PaymentErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
//        verify(paymentProductRepository, times(1)).findById(anyLong());
//        verify(userService, never()).findUserById(anyLong());
//        verify(paymentOrderRepository, never()).save(any(PaymentOrder.class));
//    }
//
//    @Test
//    @DisplayName("비활성화된 상품으로 주문 생성 시 예외 발생")
//    void createOrderWithInactiveProduct() {
//        // given
//        PaymentProduct inactiveProduct = PaymentProduct.builder()
//                .points(1000)
//                .amount(10000)
//                .isPromotion(false)
//                .build();
//
//        try {
//            java.lang.reflect.Field idField = inactiveProduct.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(inactiveProduct, 2L);
//
//            java.lang.reflect.Field activeField = inactiveProduct.getClass().getDeclaredField("active");
//            activeField.setAccessible(true);
//            activeField.set(inactiveProduct, false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        when(paymentProductRepository.findById(anyLong())).thenReturn(Optional.of(inactiveProduct));
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.createOrder(testUser.getId(), inactiveProduct.getId());
//        });
//
//        assertEquals(PaymentErrorCode.INACTIVE_PRODUCT, exception.getErrorCode());
//        verify(paymentProductRepository, times(1)).findById(anyLong());
//        verify(userService, never()).findUserById(anyLong());
//        verify(paymentOrderRepository, never()).save(any(PaymentOrder.class));
//    }
//
//    @Test
//    @DisplayName("결제를 확인하고 포인트 적립")
//    void confirmPayment() {
//        // given
//        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
//                .paymentKey(paymentKey)
//                .orderId(orderId)
//                .amount(testProduct.getAmount())
//                .build();
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
//
//        ResponseEntity<Map> mockResponse = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
//        when(restTemplate.exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        )).thenReturn(mockResponse);
//
//        PointTransactionResponse pointResponse = PointTransactionResponse.builder()
//                .id(1L)
//                .transactionType(TransactionType.EARN)
//                .amount(testProduct.getPoints())
//                .reasonType(PointReasonType.PURCHASE)
//                .entityId(testOrder.getId())
//                .balance(testProduct.getPoints())
//                .createdAt(LocalDateTime.now())
//                .build();
//
//
//        when(pointService.addPoints(eq(testUser.getId()), argThat(req ->
//                req.getAmount() == testProduct.getPoints() &&
//                        req.getReasonType().equals(PointReasonType.PURCHASE) &&
//                        req.getEntityId().equals(testOrder.getId())
//        ))).thenReturn(pointResponse);
//
//        // when
//        PaymentConfirmResponse result = paymentService.confirmPayment(request);
//
//        // then
//        assertNotNull(result);
//        assertEquals(orderId, result.getOrderId());
//        assertEquals("success", result.getStatus());
//        assertEquals(paymentKey, result.getPaymentKey());
//        assertEquals(testProduct.getAmount(), result.getAmount());
//        assertEquals(testProduct.getPoints(), result.getAddedPoints());
//
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//        verify(restTemplate, times(1)).exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        );
//
//        verify(pointService, times(1)).addPoints(
//                eq(testUser.getId()),
//                argThat(req ->
//                        req.getAmount()==testProduct.getPoints() &&
//                                req.getReasonType().equals(PointReasonType.PURCHASE) &&
//                                req.getEntityId().equals(testOrder.getId())
//                )
//        );
//        verify(paymentOrderRepository, times(1)).save(any(PaymentOrder.class));
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 주문으로 결제 확인 시 예외 발생")
//    void confirmPaymentWithNonExistingOrder() {
//        // given
//        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
//                .paymentKey(paymentKey)
//                .orderId("non_existing_order_id")
//                .amount(testProduct.getAmount())
//                .build();
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.empty());
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.confirmPayment(request);
//        });
//
//        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
//        verify(paymentOrderRepository, times(1)).findByOrderId(anyString());
//        verify(restTemplate, never()).exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        );
//    }
//
//    @Test
//    @DisplayName("금액이 일치하지 않는 결제 확인 시 예외 발생")
//    void confirmPaymentWithUnmatchedAmount() {
//        // given
//        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
//                .paymentKey(paymentKey)
//                .orderId(orderId)
//                .amount(9999)
//                .build();
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.confirmPayment(request);
//        });
//
//        assertEquals(PaymentErrorCode.UNMATCHED_AMOUNT, exception.getErrorCode());
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//        verify(restTemplate, never()).exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        );
//    }
//
//    @Test
//    @DisplayName("이미 처리된 주문으로 결제 확인 시 예외 발생")
//    void confirmPaymentWithAlreadyProcessedOrder() {
//        // given
//        PaymentOrder completedOrder = PaymentOrder.builder()
//                .user(testUser)
//                .product(testProduct)
//                .orderId(orderId)
//                .amount(testProduct.getAmount())
//                .status(PaymentStatus.DONE)
//                .build();
//
//        try {
//            java.lang.reflect.Field idField = completedOrder.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(completedOrder, 1L);
//
//            java.lang.reflect.Field paymentKeyField = completedOrder.getClass().getDeclaredField("paymentKey");
//            paymentKeyField.setAccessible(true);
//            paymentKeyField.set(completedOrder, paymentKey);
//
//            java.lang.reflect.Field paidAtField = completedOrder.getClass().getDeclaredField("paidAt");
//            paidAtField.setAccessible(true);
//            paidAtField.set(completedOrder, LocalDateTime.now());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
//                .paymentKey(paymentKey)
//                .orderId(orderId)
//                .amount(testProduct.getAmount())
//                .build();
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(completedOrder));
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.confirmPayment(request);
//        });
//
//        assertEquals(PaymentErrorCode.ALREADY_PROCESSED_ORDER, exception.getErrorCode());
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//        verify(restTemplate, never()).exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        );
//    }
//
//    @Test
//    @DisplayName("결제 확인 API 호출 실패 시 예외 발생")
//    void confirmPaymentWithAPIFailure() {
//        // given
//        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
//                .paymentKey(paymentKey)
//                .orderId(orderId)
//                .amount(testProduct.getAmount())
//                .build();
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
//
//        when(restTemplate.exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        )).thenThrow(new RuntimeException("API 호출 실패"));
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.confirmPayment(request);
//        });
//
//        assertEquals(PaymentErrorCode.FAILED_CONFIRM_PAYMENT, exception.getErrorCode());
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//        verify(restTemplate, times(1)).exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        );
//        verify(paymentOrderRepository, times(1)).save(any(PaymentOrder.class));
//
//        assertEquals(PaymentStatus.FAILED, testOrder.getStatus());
//    }
//
//    @Test
//    @DisplayName("결제 설정 정보 조회")
//    void getPaymentConfig() {
//        // when
//        PaymentConfigResponse result = paymentService.getPaymentConfig();
//
//        // then
//        assertNotNull(result);
//        assertEquals("test_client_key", result.getClientKey());
//        assertEquals("https://example.com/success", result.getSuccessCallbackUrl());
//        assertEquals("https://example.com/fail", result.getFailCallbackUrl());
//
//        verify(tossPaymentsConfig, times(1)).getClientKey();
//        verify(tossPaymentsConfig, times(1)).getSuccessCallbackUrl();
//        verify(tossPaymentsConfig, times(1)).getFailCallbackUrl();
//    }
//
//    @Test
//    @DisplayName("사용자의 결제 내역을 페이지네이션으로 조회")
//    void getUserPaymentHistoryWithPagination() {
//        // given
//        int page = 0;
//        int size = 10;
//
//        List<PaymentOrder> orders = new ArrayList<>();
//        for (int i = 1; i <= 25; i++) {
//            PaymentOrder order = PaymentOrder.builder()
//                    .user(testUser)
//                    .product(testProduct)
//                    .orderId("order_" + i)
//                    .amount(testProduct.getAmount())
//                    .status(i % 3 == 0 ? PaymentStatus.DONE : i % 3 == 1 ? PaymentStatus.READY : PaymentStatus.FAILED)
//                    .build();
//
//            try {
//                java.lang.reflect.Field idField = order.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(order, (long) i);
//
//                java.lang.reflect.Field createdAtField = order.getClass().getSuperclass().getDeclaredField("createdAt");
//                createdAtField.setAccessible(true);
//                createdAtField.set(order, LocalDateTime.now().minusDays(i));
//
//                if (order.getStatus() == PaymentStatus.DONE) {
//                    java.lang.reflect.Field paymentKeyField = order.getClass().getDeclaredField("paymentKey");
//                    paymentKeyField.setAccessible(true);
//                    paymentKeyField.set(order, "payment_key_" + i);
//
//                    java.lang.reflect.Field paidAtField = order.getClass().getDeclaredField("paidAt");
//                    paidAtField.setAccessible(true);
//                    paidAtField.set(order, LocalDateTime.now().minusDays(i).plusHours(1));
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            orders.add(order);
//        }
//
//        PageImpl<PaymentOrder> orderPage = new PageImpl<>(
//                orders.subList(0, Math.min(size, orders.size())),
//                PageRequest.of(page, size, Sort.by("createdAt").descending()),
//                orders.size()
//        );
//
//        when(userService.findUserById(testUser.getId())).thenReturn(testUser);
//        when(paymentOrderRepository.findByUserIdWithProductOrderByCreatedAtDesc(
//                eq(testUser.getId()), any(Pageable.class)
//        )).thenReturn(orderPage);
//
//        // when
//        Page<PaymentHistoryResponse> result = paymentService.getUserPaymentHistory(testUser.getId(), page, size);
//
//        // then
//        assertNotNull(result);
//        assertEquals(size, result.getContent().size());
//        assertEquals(orders.size(), result.getTotalElements());
//        assertEquals((int) Math.ceil((double) orders.size() / size), result.getTotalPages());
//        assertEquals(page, result.getNumber());
//
//        for (int i = 0; i < result.getContent().size() - 1; i++) {
//            assertTrue(
//                    result.getContent().get(i).getCreatedAt()
//                            .isAfter(result.getContent().get(i + 1).getCreatedAt())
//            );
//        }
//
//        for (int i = 0; i < result.getContent().size(); i++) {
//            PaymentHistoryResponse response = result.getContent().get(i);
//            PaymentOrder order = orders.get(i);
//
//            assertEquals(order.getId(), response.getId());
//            assertEquals(order.getOrderId(), response.getOrderId());
//            assertEquals(order.getProduct().getPoints(), response.getPoints());
//            assertEquals(order.getAmount(), response.getAmount());
//            assertEquals(order.getStatus(), response.getStatus());
//            assertEquals(order.getPaidAt(), response.getPaidAt());
//            assertEquals(order.getCreatedAt(), response.getCreatedAt());
//        }
//
//        verify(userService, times(1)).findUserById(testUser.getId());
//        verify(paymentOrderRepository, times(1)).findByUserIdWithProductOrderByCreatedAtDesc(
//                eq(testUser.getId()), any(Pageable.class)
//        );
//    }
//
//    @Test
//    @DisplayName("사용자의 결제 내역 두 번째 페이지 조회")
//    void getUserPaymentHistorySecondPage() {
//        // given
//        int page = 1;
//        int size = 5;
//
//        List<PaymentOrder> allOrders = new ArrayList<>();
//        for (int i = 1; i <= 12; i++) {
//            PaymentOrder order = PaymentOrder.builder()
//                    .user(testUser)
//                    .product(testProduct)
//                    .orderId("order_" + i)
//                    .amount(testProduct.getAmount())
//                    .status(PaymentStatus.DONE)
//                    .build();
//
//            try {
//                java.lang.reflect.Field idField = order.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(order, (long) i);
//
//                java.lang.reflect.Field createdAtField = order.getClass().getSuperclass().getDeclaredField("createdAt");
//                createdAtField.setAccessible(true);
//                createdAtField.set(order, LocalDateTime.now().minusDays(i));
//
//                java.lang.reflect.Field paymentKeyField = order.getClass().getDeclaredField("paymentKey");
//                paymentKeyField.setAccessible(true);
//                paymentKeyField.set(order, "payment_key_" + i);
//
//                java.lang.reflect.Field paidAtField = order.getClass().getDeclaredField("paidAt");
//                paidAtField.setAccessible(true);
//                paidAtField.set(order, LocalDateTime.now().minusDays(i).plusHours(1));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            allOrders.add(order);
//        }
//
//        List<PaymentOrder> secondPageOrders = allOrders.subList(size, 2 * size);
//
//        PageImpl<PaymentOrder> orderPage = new PageImpl<>(
//                secondPageOrders,
//                PageRequest.of(page, size, Sort.by("createdAt").descending()),
//                allOrders.size()
//        );
//
//        when(userService.findUserById(testUser.getId())).thenReturn(testUser);
//        when(paymentOrderRepository.findByUserIdWithProductOrderByCreatedAtDesc(
//                eq(testUser.getId()), any(Pageable.class)
//        )).thenReturn(orderPage);
//
//        // when
//        Page<PaymentHistoryResponse> result = paymentService.getUserPaymentHistory(testUser.getId(), page, size);
//
//        // then
//        assertNotNull(result);
//        assertEquals(size, result.getContent().size());
//        assertEquals(allOrders.size(), result.getTotalElements());
//        assertEquals((int) Math.ceil((double) allOrders.size() / size), result.getTotalPages());
//        assertEquals(page, result.getNumber());
//
//        for (int i = 0; i < result.getContent().size(); i++) {
//            PaymentHistoryResponse response = result.getContent().get(i);
//            PaymentOrder order = secondPageOrders.get(i);
//
//            assertEquals(order.getId(), response.getId());
//            assertEquals(order.getOrderId(), response.getOrderId());
//        }
//
//        verify(userService, times(1)).findUserById(testUser.getId());
//        verify(paymentOrderRepository, times(1)).findByUserIdWithProductOrderByCreatedAtDesc(
//                eq(testUser.getId()),
//                argThat(pageable ->
//                        pageable.getPageNumber() == page &&
//                                pageable.getPageSize() == size &&
//                                pageable.getSort().equals(Sort.by("createdAt").descending())
//                )
//        );
//    }
//
//    @Test
//    @DisplayName("데이터가 없는 경우 빈 페이지 반환")
//    void getUserPaymentHistoryEmptyPage() {
//        // given
//        int page = 0;
//        int size = 10;
//
//        Page<PaymentOrder> emptyPage = new PageImpl<>(
//                Collections.emptyList(),
//                PageRequest.of(page, size),
//                0
//        );
//
//        when(userService.findUserById(testUser.getId())).thenReturn(testUser);
//        when(paymentOrderRepository.findByUserIdWithProductOrderByCreatedAtDesc(
//                eq(testUser.getId()), any(Pageable.class)
//        )).thenReturn(emptyPage);
//
//        // when
//        Page<PaymentHistoryResponse> result = paymentService.getUserPaymentHistory(testUser.getId(), page, size);
//
//        // then
//        assertNotNull(result);
//        assertTrue(result.getContent().isEmpty());
//        assertEquals(0, result.getTotalElements());
//        assertEquals(0, result.getTotalPages());
//
//        verify(userService, times(1)).findUserById(testUser.getId());
//        verify(paymentOrderRepository, times(1)).findByUserIdWithProductOrderByCreatedAtDesc(
//                eq(testUser.getId()), any(Pageable.class)
//        );
//    }
//
//    @Test
//    @DisplayName("결제 내역 응답에 올바른 데이터 포함")
//    void paymentHistoryResponseContainsCorrectData() {
//        // given
//        PaymentOrder order = PaymentOrder.builder()
//                .user(testUser)
//                .product(testProduct)
//                .orderId("test_order_id")
//                .amount(10000)
//                .status(PaymentStatus.DONE)
//                .build();
//
//        try {
//            java.lang.reflect.Field idField = order.getClass().getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(order, 1L);
//
//            java.lang.reflect.Field createdAtField = order.getClass().getSuperclass().getDeclaredField("createdAt");
//            createdAtField.setAccessible(true);
//            createdAtField.set(order, LocalDateTime.now().minusDays(1));
//
//            java.lang.reflect.Field paymentKeyField = order.getClass().getDeclaredField("paymentKey");
//            paymentKeyField.setAccessible(true);
//            paymentKeyField.set(order, "payment_key_test");
//
//            java.lang.reflect.Field paidAtField = order.getClass().getDeclaredField("paidAt");
//            paidAtField.setAccessible(true);
//            paidAtField.set(order, LocalDateTime.now());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // when
//        PaymentHistoryResponse response = PaymentHistoryResponse.from(order);
//
//        // then
//        assertNotNull(response);
//        assertEquals(order.getId(), response.getId());
//        assertEquals(order.getOrderId(), response.getOrderId());
//        assertEquals(order.getProduct().getPoints(), response.getPoints());
//        assertEquals(order.getAmount(), response.getAmount());
//        assertEquals(order.getStatus(), response.getStatus());
//        assertEquals(order.getPaidAt(), response.getPaidAt());
//        assertEquals(order.getCreatedAt(), response.getCreatedAt());
//    }
//
//    @Test
//    @DisplayName("결제 상세 정보 조회")
//    void getPaymentDetail() {
//        // given
//        testOrder.completePayment(paymentKey);
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//
//        // when
//        PaymentDetailResponse result = paymentService.getPaymentDetail(testUser.getId(), orderId);
//
//        // then
//        assertNotNull(result);
//        assertEquals(testOrder.getId(), result.getId());
//        assertEquals(testOrder.getOrderId(), result.getOrderId());
//        assertEquals(testOrder.getPaymentKey(), result.getPaymentKey());
//        assertEquals(testProduct.getPoints(), result.getPoints());
//        assertEquals(testOrder.getAmount(), result.getAmount());
//        assertEquals(testOrder.getStatus(), result.getStatus());
//        assertEquals(testOrder.getPaidAt(), result.getPaidAt());
//
//        assertNotNull(result.getReceiptUrl());
//        assertTrue(result.getReceiptUrl().contains(paymentKey));
//
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//    }
//
//    @Test
//    @DisplayName("존재하지 않는 주문의 상세 정보 조회 시 예외 발생")
//    void getPaymentDetailWithNonExistingOrder() {
//        // given
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.empty());
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.getPaymentDetail(testUser.getId(), "non_existing_order_id");
//        });
//
//        assertEquals(PaymentErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
//        verify(paymentOrderRepository, times(1)).findByOrderId(anyString());
//    }
//
//    @Test
//    @DisplayName("다른 사용자의 주문 상세 정보 조회 시 예외 발생")
//    void getPaymentDetailWithUnauthorizedUser() {
//        // given
//        Long otherUserId = 999L;
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            paymentService.getPaymentDetail(otherUserId, orderId);
//        });
//
//        assertEquals(PaymentErrorCode.UNAUTHORIZED, exception.getErrorCode());
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//    }
//
//    @Test
//    @DisplayName("READY 상태의 주문 상세 정보 조회 시 영수증 URL 앖음")
//    void getPaymentDetailWithReadyStatus() {
//        // given
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//
//        // when
//        PaymentDetailResponse result = paymentService.getPaymentDetail(testUser.getId(), orderId);
//
//        // then
//        assertNotNull(result);
//        assertEquals(testOrder.getStatus(), PaymentStatus.READY);
//        assertNull(result.getReceiptUrl());
//
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//    }
//
//    @Test
//    @DisplayName("포인트 적립 중 낙관적 락 예외 발생 시 PointService의 재시도 로직 동작")
//    void confirmPaymentWithOptimisticLockRetry() {
//        // given
//        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
//                .paymentKey(paymentKey)
//                .orderId(orderId)
//                .amount(testProduct.getAmount())
//                .build();
//
//        when(paymentOrderRepository.findByOrderId(anyString())).thenReturn(Optional.of(testOrder));
//        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenReturn(testOrder);
//
//        ResponseEntity<Map> mockResponse = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
//        when(restTemplate.exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        )).thenReturn(mockResponse);
//
//        PointTransactionResponse pointResponse = PointTransactionResponse.builder()
//                .id(1L)
//                .transactionType(TransactionType.EARN)
//                .amount(testProduct.getPoints())
//                .reasonType(PointReasonType.PURCHASE)
//                .entityId(testOrder.getId())
//                .balance(testProduct.getPoints())
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        when(pointService.addPoints(eq(testUser.getId()), any(PointAddRequest.class)))
//                .thenThrow(new CustomException(PointErrorCode.TRANSACTION_CONFLICT))
//                .thenReturn(pointResponse);
//
//        // when
//        assertThrows(CustomException.class, () -> {
//            paymentService.confirmPayment(request);
//        });
//
//        // then
//        verify(paymentOrderRepository, times(1)).findByOrderId(orderId);
//        verify(restTemplate, times(1)).exchange(
//                anyString(),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//        );
//        verify(pointService, times(1)).addPoints(eq(testUser.getId()), any(PointAddRequest.class));
//
//        assertEquals(PaymentStatus.FAILED, testOrder.getStatus());
//    }
//}