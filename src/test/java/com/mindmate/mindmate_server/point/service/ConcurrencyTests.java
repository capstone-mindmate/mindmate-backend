//package com.mindmate.mindmate_server.point.service;
//
//import com.mindmate.mindmate_server.point.domain.PointReasonType;
//import com.mindmate.mindmate_server.point.dto.PointRequest;
//import com.mindmate.mindmate_server.point.repository.PointTransactionRepository;
//import com.mindmate.mindmate_server.user.domain.RoleType;
//import com.mindmate.mindmate_server.user.domain.User;
//import com.mindmate.mindmate_server.user.repository.UserRepository;
//import com.mindmate.mindmate_server.user.service.UserService;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.annotation.Rollback;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.TransactionDefinition;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@SpringBootTest
//@Rollback(false)  // 테스트 롤백 비활성화
//class PointServiceConcurrencyTest {
//
//    @Autowired
//    private PointService pointService;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PointTransactionRepository pointTransactionRepository;
//
//    @Autowired
//    private PlatformTransactionManager transactionManager;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    private Long testUserId;
//
//    @BeforeEach
//    void setup() {
//        TransactionTemplate template = new TransactionTemplate(transactionManager);
//        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//
//        testUserId = template.execute(status -> {
//            // 테스트용 사용자 생성
//            String uniqueEmail = "test_" + System.currentTimeMillis() + "@example.com";
//            User testUser = User.builder()
//                    .email(uniqueEmail)
//                    .password("password")
//                    .agreedToTerms(true)
//                    .role(RoleType.ROLE_USER)
//                    .build();
//
//            userService.save(testUser);
//            User savedUser = userRepository.findByEmail(uniqueEmail)
//                    .orElseThrow(() -> new RuntimeException("테스트 사용자 조회 실패"));
//
//            Long userId = savedUser.getId();
//
//            // 초기 포인트 설정 (1000 포인트)
//            pointService.addPoints(userId,
//                    PointRequest.forAddPoints(1000, PointReasonType.ADMIN_GRANTED, null));
//
//            // 초기 잔액 확인
//            int initialBalance = pointService.getCurrentBalance(userId);
//            System.out.println("초기 설정 - 사용자 ID: " + userId + ", 초기 잔액: " + initialBalance);
//
//            return userId;
//        });
//    }
//
//    @AfterEach
//    void cleanup() {
//        if (testUserId != null) {
//            TransactionTemplate template = new TransactionTemplate(transactionManager);
//            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//
//            template.execute(status -> {
//                // 테스트 생성 데이터 정리
//                try {
//                    pointTransactionRepository.deleteAllByUserId(testUserId);
//                } catch (Exception e) {
//                    System.out.println("포인트 트랜잭션 정리 중 오류: " + e.getMessage());
//                }
//
//                try {
//                    userRepository.deleteById(testUserId);
//                } catch (Exception e) {
//                    System.out.println("사용자 정리 중 오류: " + e.getMessage());
//                }
//
//                return null;
//            });
//        }
//    }
//
//    @Test
//    void testConcurrentPointOperations() throws InterruptedException {
//        System.out.println("\n===== 동시 포인트 사용 테스트 시작 =====");
//
//        // 현재 잔액 확인
//        TransactionTemplate template = new TransactionTemplate(transactionManager);
//        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//
//        int startBalance = template.execute(status -> {
//            entityManager.clear();  // 캐시 초기화
//            return pointService.getCurrentBalance(testUserId);
//        });
//
//        System.out.println("테스트 시작 시 잔액: " + startBalance);
//
//        // 동시에 여러 포인트 사용 요청 실행
//        int threadCount = 10;
//        int pointsPerOperation = 50; // 각 작업당 50 포인트
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch readyLatch = new CountDownLatch(threadCount);
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch completionLatch = new CountDownLatch(threadCount);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger retryCount = new AtomicInteger(0);
//        List<Exception> exceptions = new ArrayList<>();
//
//        for (int i = 0; i < threadCount; i++) {
//            final int index = i;
//            executorService.submit(() -> {
//                try {
//                    readyLatch.countDown();  // 준비 완료 신호
//                    startLatch.await();      // 모든 스레드가 동시에 시작하도록 대기
//
//                    System.out.println("스레드 " + index + " - 포인트 사용 시작");
//                    // 포인트 사용 요청
//                    pointService.usePoints(testUserId,
//                            PointRequest.forUsePoints(pointsPerOperation, PointReasonType.PURCHASE, null));
//                    System.out.println("스레드 " + index + " - 포인트 사용 완료");
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    synchronized (exceptions) {
//                        exceptions.add(e);
//                    }
//                    if (e.getMessage() != null && e.getMessage().contains("OptimisticLockingFailureException")) {
//                        retryCount.incrementAndGet();
//                    }
//                    System.out.println("스레드 " + index + " - 오류 발생: " + e.getMessage());
//                } finally {
//                    completionLatch.countDown();
//                }
//            });
//        }
//
//        // 모든 스레드가 준비될 때까지 대기
//        readyLatch.await();
//        // 동시에 모든 스레드 시작
//        startLatch.countDown();
//
//        // 모든 작업이 완료될 때까지 대기
//        completionLatch.await();
//        executorService.shutdown();
//
//        // 결과가 데이터베이스에 완전히 반영될 시간을 줌
//        Thread.sleep(500);
//
//        // 새 트랜잭션에서 최종 잔액 조회
//        Integer finalBalance = template.execute(status -> {
//            entityManager.clear();  // 캐시 초기화 중요
//            return pointService.getCurrentBalance(testUserId);
//        });
//
//        int expectedBalance = startBalance - (successCount.get() * pointsPerOperation);
//
//        System.out.println("\n===== 동시 포인트 사용 테스트 결과 =====");
//        System.out.println("시작 잔액: " + startBalance);
//        System.out.println("성공한 작업 수: " + successCount.get());
//        System.out.println("낙관적 락 충돌/재시도 횟수: " + retryCount.get());
//        System.out.println("실패한 작업 수: " + exceptions.size());
//        System.out.println("기대 잔액: " + expectedBalance);
//        System.out.println("실제 잔액: " + finalBalance);
//
//        // 실패한 작업들의 예외 메시지 출력
//        if (!exceptions.isEmpty()) {
//            System.out.println("\n발생한 예외들:");
//            for (Exception e : exceptions) {
//                System.out.println("- " + e.getClass().getSimpleName() + ": " + e.getMessage());
//            }
//        }
//
//        // 최종 잔액 검증
//        assertEquals(expectedBalance, finalBalance,
//                "포인트 사용 후 잔액이 예상과 일치하지 않습니다. 낙관적 락과 트랜잭션이 제대로 작동하지 않을 수 있습니다.");
//    }
//
//    // 다른 테스트 메서드도 동일한 방식으로 수정
//}