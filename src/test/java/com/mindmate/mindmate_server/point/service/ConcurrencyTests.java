package com.mindmate.mindmate_server.point.service;

import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.chat.repository.ChatMessageRepository;
import com.mindmate.mindmate_server.chat.repository.ChatRoomRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineLikeRepository;
import com.mindmate.mindmate_server.magazine.repository.MagazineRepository;
import com.mindmate.mindmate_server.matching.domain.Matching;
import com.mindmate.mindmate_server.matching.repository.MatchingRepository;
import com.mindmate.mindmate_server.matching.repository.WaitingUserRepository;
import com.mindmate.mindmate_server.payment.domain.PaymentProduct;
import com.mindmate.mindmate_server.payment.dto.PaymentConfirmRequest;
import com.mindmate.mindmate_server.payment.dto.PaymentConfirmResponse;
import com.mindmate.mindmate_server.payment.dto.PaymentOrderResponse;
import com.mindmate.mindmate_server.payment.repository.PaymentOrderRepository;
import com.mindmate.mindmate_server.payment.repository.PaymentProductRepository;
import com.mindmate.mindmate_server.payment.service.PaymentService;
import com.mindmate.mindmate_server.point.domain.PointReasonType;
import com.mindmate.mindmate_server.point.domain.TransactionType;
import com.mindmate.mindmate_server.point.dto.PointRequest;
import com.mindmate.mindmate_server.point.repository.PointTransactionRepository;
import com.mindmate.mindmate_server.review.repository.ReviewRepository;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.ProfileImage;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.repository.ProfileImageRepository;
import com.mindmate.mindmate_server.user.repository.ProfileRepository;
import com.mindmate.mindmate_server.user.repository.UserRepository;
import com.mindmate.mindmate_server.user.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(ConcurrencyTests.TestConfig.class)
public class ConcurrencyTests {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PointService pointService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileImageRepository profileImageRepository;

    @Autowired
    private PaymentProductRepository paymentProductRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private WaitingUserRepository waitingUserRepository;

    @Autowired
    private MatchingRepository matchingRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MagazineLikeRepository magazineLikeRepository;

    @Autowired
    private MagazineRepository magazineRepository;

    private User testUser;
    private Profile testProfile;
    private PaymentProduct testProduct;
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RestTemplate mockRestTemplate() {
            RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("paymentKey", "test_payment_key");
            responseBody.put("orderId", "test_order_id");
            responseBody.put("status", "DONE");

            ResponseEntity<Map> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);

            Mockito.when(mockRestTemplate.exchange(
                    Mockito.anyString(),
                    Mockito.eq(HttpMethod.POST),
                    Mockito.any(),
                    Mockito.eq(Map.class)
            )).thenReturn(mockResponse);

            return mockRestTemplate;
        }
    }

    @BeforeEach
    public void setup() {

        testUser = User.builder()
                .email("test_" + UUID.randomUUID().toString() + "@example.com")
                .provider(AuthProvider.GOOGLE)
                .role(RoleType.ROLE_ADMIN)
                .build();
        testUser = userRepository.save(testUser);

        ProfileImage profileImage = ProfileImage.builder()
                .imageUrl("https://example.com/profile-image.jpg")
                .storedName("default-profile")
                .originalName("default-original")
                .contentType("image/webp")
                .build();
        profileImage = profileImageRepository.save(profileImage);

        testProfile = Profile.builder()
                .user(testUser)
                .agreedToTerms(true)
                .nickname("test_" + UUID.randomUUID().toString())
                .department("Computer Science")
                .entranceTime(2020)
                .graduation(false)
                .profileImage(profileImage)
                .build();
        testProfile = profileRepository.save(testProfile);

        testProduct = PaymentProduct.builder()
                .points(1000)
                .amount(10000)
                .isPromotion(false)
                .build();
        testProduct = paymentProductRepository.save(testProduct);
    }

    @AfterEach
    public void cleanup() {
        try {
            waitingUserRepository.deleteAll();

            reviewRepository.deleteAll();

            chatMessageRepository.deleteAll();

            List<Matching> matchings = matchingRepository.findAll();
            for (Matching matching : matchings) {
                matching.setChatRoom(null);
            }
            matchingRepository.saveAll(matchings);

            if (chatRoomRepository != null) {
                chatRoomRepository.deleteAll();
            }
            matchingRepository.deleteAll();

            pointTransactionRepository.deleteAll();

            paymentOrderRepository.deleteAll();

            magazineLikeRepository.deleteAll();
            magazineRepository.deleteAll();

            paymentProductRepository.deleteAll();

            profileRepository.deleteAll();

            userRepository.deleteAll();

            profileImageRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("테스트 데이터 정리 중 오류 발생: " + e.getMessage());
        }
    }

    @Test
    @Timeout(value = 10)
    public void testPaymentIdempotency() throws InterruptedException {
        PaymentOrderResponse orderResponse = paymentService.createOrder(testUser.getId(), testProduct.getId());

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderResponse.getOrderId())
                .paymentKey("test_payment_key_" + UUID.randomUUID().toString())
                .amount(orderResponse.getAmount())
                .build();

        PaymentConfirmResponse firstResponse = paymentService.confirmPayment(request);

        PaymentConfirmResponse secondResponse = paymentService.confirmPayment(request);

        assertEquals(firstResponse.getOrderId(), secondResponse.getOrderId());
        assertEquals(firstResponse.getPaymentKey(), secondResponse.getPaymentKey());
        assertEquals(firstResponse.getAmount(), secondResponse.getAmount());
        assertEquals(firstResponse.getAddedPoints(), secondResponse.getAddedPoints());

        int userPoints = pointService.getCurrentBalance(testUser.getId());
        assertEquals(orderResponse.getPoints().intValue(), userPoints);
    }

    @Test
    @Timeout(value = 30)
    public void testPointServiceRetryMechanism() throws InterruptedException {
        final int threadCount = 3;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);

        final int pointsPerThread = 100;
        final AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        int initialPoints = pointService.getCurrentBalance(testUser.getId());

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    try {
                        PointRequest request = PointRequest.builder()
                                .transactionType(TransactionType.EARN)
                                .amount(pointsPerThread)
                                .reasonType(PointReasonType.ADMIN_GRANTED)
                                .entityId(null)
                                .build();

                        pointService.addPoints(testUser.getId(), request);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        Thread.sleep(100);
        startLatch.countDown();

        endLatch.await();
        executorService.shutdown();

        Thread.sleep(500);

        assertEquals(threadCount, successCount.get());

        int expectedPoints = initialPoints + (pointsPerThread * successCount.get());
        int actualPoints = pointService.getCurrentBalance(testUser.getId());

        assertEquals(expectedPoints, actualPoints);
    }

    @Test
    @Timeout(value = 10)
    public void testMultiplePaymentRequestsWithSamePaymentKey() throws InterruptedException {

        PaymentOrderResponse orderResponse = paymentService.createOrder(testUser.getId(), testProduct.getId());

        final String sharedPaymentKey = "test_shared_payment_key_" + UUID.randomUUID();

        final int threadCount = 3;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger idempotentSuccessCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                            .orderId(orderResponse.getOrderId())
                            .paymentKey(sharedPaymentKey)
                            .amount(orderResponse.getAmount())
                            .build();

                    try {
                        PaymentConfirmResponse response = paymentService.confirmPayment(request);
                        if (response != null) {
                            successCount.incrementAndGet();
                            if ("success".equals(response.getStatus())) {
                                idempotentSuccessCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null &&
                                (e.getMessage().contains("결제 승인에 실패했습니다") ||
                                        e.getMessage().contains("Duplicate entry"))) {
                            System.out.println("예상된 중복 처리 오류: " + e.getMessage());
                        } else {
                            errorCount.incrementAndGet();
                            System.err.println("예상치 못한 오류: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        endLatch.await();
        executorService.shutdown();

        System.out.println("성공: " + successCount.get() +
                ", 멱등성 성공: " + idempotentSuccessCount.get() +
                ", 오류: " + errorCount.get());

        assertTrue(successCount.get() >= 1, "최소 1개의 결제는 성공해야 함");

        int userPoints = pointService.getCurrentBalance(testUser.getId());
        assertEquals(orderResponse.getPoints().intValue(), userPoints,
                "포인트는 정확히 한 번만 적립되어야 함");
    }

    @Test
    @Timeout(value = 10)
    public void testDifferentPaymentKeysForSameOrder() {

        PaymentOrderResponse orderResponse = paymentService.createOrder(testUser.getId(), testProduct.getId());

        PaymentConfirmRequest firstRequest = PaymentConfirmRequest.builder()
                .orderId(orderResponse.getOrderId())
                .paymentKey("first_payment_key")
                .amount(orderResponse.getAmount())
                .build();

        PaymentConfirmResponse firstResponse = paymentService.confirmPayment(firstRequest);
        assertNotNull(firstResponse);
        assertEquals("success", firstResponse.getStatus());

        PaymentConfirmRequest secondRequest = PaymentConfirmRequest.builder()
                .orderId(orderResponse.getOrderId())
                .paymentKey("different_payment_key")
                .amount(orderResponse.getAmount())
                .build();

        PaymentConfirmResponse secondResponse = paymentService.confirmPayment(secondRequest);

        assertNotNull(secondResponse);
        assertEquals(firstResponse.getOrderId(), secondResponse.getOrderId());
        assertEquals(firstResponse.getPaymentKey(), secondResponse.getPaymentKey());
        assertEquals("success", secondResponse.getStatus());

        int userPoints = pointService.getCurrentBalance(testUser.getId());
        assertEquals(orderResponse.getPoints().intValue(), userPoints);
    }
}