//package com.mindmate.mindmate_server.point.service;
//
//import com.mindmate.mindmate_server.global.exception.CustomException;
//import com.mindmate.mindmate_server.global.exception.PointErrorCode;
//import com.mindmate.mindmate_server.point.domain.PointReasonType;
//import com.mindmate.mindmate_server.point.domain.PointTransaction;
//import com.mindmate.mindmate_server.point.domain.TransactionType;
//import com.mindmate.mindmate_server.point.dto.PointSummaryResponse;
//import com.mindmate.mindmate_server.point.dto.PointTransactionResponse;
//import com.mindmate.mindmate_server.point.repository.PointTransactionRepository;
//import com.mindmate.mindmate_server.user.domain.RoleType;
//import com.mindmate.mindmate_server.user.domain.User;
//import com.mindmate.mindmate_server.user.service.UserService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.OptimisticLockingFailureException;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PointServiceImplTest {
//
//    @Mock
//    private PointTransactionRepository pointTransactionRepository;
//
//    @Mock
//    private UserService userService;
//
//    @InjectMocks
//    private PointServiceImpl pointService;
//
//    private User mockUser() {
//        return User.builder()
//                .email("test@example.com")
//                .password("password")
//                .agreedToTerms(true)
//                .role(RoleType.ROLE_PROFILE)
//                .build();
//    }
//
//    @Test
//    @DisplayName("포인트 적립 성공")
//    void addPoints_success() {
//        Long userId = 1L;
//        PointAddRequest request = PointAddRequest.builder()
//                .amount(100)
//                .reasonType(PointReasonType.REVIEW_WRITTEN)
//                .entityId(1L)
//                .build();
//
//        User user = mockUser();
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findTopByUserOrderByVersionDesc(user)).thenReturn(Optional.empty());
//        when(pointTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
//
//        PointTransactionResponse response = pointService.addPoints(userId, request);
//
//        assertThat(response.getAmount()).isEqualTo(100);
//        assertThat(response.getTransactionType()).isEqualTo(TransactionType.EARN);
//    }
//
//    @Test
//    @DisplayName("포인트 적립 실패 - 금액ㅁ")
//    void addPoints_invalidAmount() {
//        Long userId = 1L;
//        PointAddRequest request = PointAddRequest.builder()
//                .amount(0)
//                .reasonType(PointReasonType.REVIEW_WRITTEN)
//                .entityId(1L)
//                .build();
//
//        assertThatThrownBy(() -> pointService.addPoints(userId, request))
//                .isInstanceOf(CustomException.class)
//                .hasMessageContaining(PointErrorCode.INVALID_POINT_AMOUNT.getMessage());
//    }
//
//    @Test
//    @DisplayName("포인트 사용 성공")
//    void usePoints_success() {
//        Long userId = 1L;
//        PointUseRequest request = PointUseRequest.builder()
//                .amount(50)
//                .reasonType(PointReasonType.EMOTICON_PURCHASED)
//                .entityId(1L)
//                .build();
//
//        User user = mockUser();
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findTopByUserOrderByVersionDesc(user)).thenReturn(Optional.of(mockBalance(100)));
//        when(pointTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
//
//        PointTransactionResponse response = pointService.usePoints(userId, request);
//
//        assertThat(response.getAmount()).isEqualTo(50);
//        assertThat(response.getTransactionType()).isEqualTo(TransactionType.SPEND);
//    }
//
//    @Test
//    @DisplayName("포인트 사용 실패 - 잔액 부족")
//    void usePoints_insufficientBalance() {
//        Long userId = 1L;
//        PointUseRequest request = PointUseRequest.builder()
//                .amount(150)
//                .reasonType(PointReasonType.EMOTICON_PURCHASED)
//                .entityId(1L)
//                .build();
//
//        User user = mockUser();
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findTopByUserOrderByVersionDesc(user)).thenReturn(Optional.of(mockBalance(100)));
//
//        assertThatThrownBy(() -> pointService.usePoints(userId, request))
//                .isInstanceOf(CustomException.class)
//                .hasMessageContaining(PointErrorCode.INSUFFICIENT_POINTS.getMessage());
//    }
//
//    @Test
//    @DisplayName("현재 포인트 잔액 조회 성공")
//    void getCurrentBalance_success() {
//        Long userId = 1L;
//        User user = mockUser();
//
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findTopByUserOrderByVersionDesc(user)).thenReturn(Optional.of(mockBalance(200)));
//
//        int balance = pointService.getCurrentBalance(userId);
//
//        assertThat(balance).isEqualTo(200);
//    }
//
//    @Test
//    @DisplayName("거래 내역 조회 성공")
//    void getTransactionHistory_success() {
//        Long userId = 1L;
//        User user = mockUser();
//
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findByUserOrderByCreatedAtDesc(eq(user), any())).thenReturn(new PageImpl<>(Collections.singletonList(mockBalance(50))));
//
//        Page<PointTransactionResponse> history = pointService.getTransactionHistory(userId, PageRequest.of(0, 10));
//
//        assertThat(history.getTotalElements()).isEqualTo(1);
//    }
//
//    @Test
//    @DisplayName("기간별 적립 포인트 조회 성공")
//    void getTotalEarnedPointsInPeriod_success() {
//        Long userId = 1L;
//        User user = mockUser();
//
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findByUserAndCreatedAtBetweenAndTransactionType(any(), any(), any(), eq(TransactionType.EARN)))
//                .thenReturn(Collections.singletonList(mockBalance(300)));
//
//        int total = pointService.getTotalEarnedPointsInPeriod(userId, LocalDateTime.now().minusDays(7), LocalDateTime.now());
//
//        assertThat(total).isEqualTo(300);
//    }
//
//    @Test
//    @DisplayName("기간별 사용 포인트 조회 성공")
//    void getTotalSpentPointsInPeriod_success() {
//        Long userId = 1L;
//        User user = mockUser();
//
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findByUserAndCreatedAtBetweenAndTransactionType(any(), any(), any(), eq(TransactionType.SPEND)))
//                .thenReturn(Collections.singletonList(mockBalance(100)));
//
//        int total = pointService.getTotalSpentPointsInPeriod(userId, LocalDateTime.now().minusDays(7), LocalDateTime.now());
//
//        assertThat(total).isEqualTo(100);
//    }
//
//    @Test
//    @DisplayName("전체 적립 포인트 조회 성공")
//    void getTotalEarnedPoints_success() {
//        Long userId = 1L;
//        when(pointTransactionRepository.sumAmountByUserIdAndTransactionType(userId, TransactionType.EARN)).thenReturn(500);
//
//        int total = pointService.getTotalEarnedPoints(userId);
//
//        assertThat(total).isEqualTo(500);
//    }
//
//    @Test
//    @DisplayName("전체 사용 포인트 조회 성공")
//    void getTotalSpentPoints_success() {
//        Long userId = 1L;
//        when(pointTransactionRepository.sumAmountByUserIdAndTransactionType(userId, TransactionType.SPEND)).thenReturn(250);
//
//        int total = pointService.getTotalSpentPoints(userId);
//
//        assertThat(total).isEqualTo(250);
//    }
//
//    @Test
//    @DisplayName("포인트 요약 조회 성공")
//    void getUserPointSummary_success() {
//        Long userId = 1L;
//
//        when(pointTransactionRepository.findTopByUserOrderByVersionDesc(any())).thenReturn(Optional.of(mockBalance(200)));
//        when(pointTransactionRepository.sumAmountByUserIdAndTransactionType(userId, TransactionType.EARN)).thenReturn(1000);
//        when(pointTransactionRepository.sumAmountByUserIdAndTransactionType(userId, TransactionType.SPEND)).thenReturn(800);
//        when(userService.findUserById(userId)).thenReturn(mockUser());
//
//        PointSummaryResponse summary = pointService.getUserPointSummary(userId);
//
//        assertThat(summary.getCurrentBalance()).isEqualTo(200);
//        assertThat(summary.getTotalEarned()).isEqualTo(1000);
//        assertThat(summary.getTotalSpent()).isEqualTo(800);
//    }
//
//    @Test
//    @DisplayName("OptimisticLockingFailureException 발생 시 재시도 후 성공")
//    void retryOnOptimisticLockingFailure() {
//        Long userId = 1L;
//        PointAddRequest request = PointAddRequest.builder()
//                .amount(100)
//                .reasonType(PointReasonType.REVIEW_WRITTEN)
//                .entityId(1L)
//                .build();
//
//        User user = mockUser();
//        when(userService.findUserById(userId)).thenReturn(user);
//        when(pointTransactionRepository.findTopByUserOrderByVersionDesc(user)).thenReturn(Optional.empty());
//        when(pointTransactionRepository.save(any()))
//                .thenThrow(new OptimisticLockingFailureException("Optimistic lock exception"))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        PointTransactionResponse response = pointService.addPoints(userId, request);
//
//        assertThat(response.getAmount()).isEqualTo(100);
//    }
//
//    private PointTransaction mockBalance(int balance) {
//        return PointTransaction.builder()
//                .balance(balance)
//                .amount(balance)
//                .transactionType(TransactionType.EARN)
//                .reasonType(PointReasonType.REVIEW_WRITTEN)
//                .build();
//    }
//}