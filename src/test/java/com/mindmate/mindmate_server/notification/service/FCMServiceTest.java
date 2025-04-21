package com.mindmate.mindmate_server.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.notification.domain.FCMToken;
import com.mindmate.mindmate_server.notification.domain.NotificationType;
import com.mindmate.mindmate_server.notification.dto.FCMTokenRequest;
import com.mindmate.mindmate_server.notification.dto.FCMTokenResponse;
import com.mindmate.mindmate_server.notification.dto.NotificationEvent;
import com.mindmate.mindmate_server.notification.repository.FCMTokenRepository;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FCMServiceTest {

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private FCMService fcmService;

    private User testUser;
    private FCMToken testToken;
    private NotificationEvent testEvent;

    @BeforeEach
    void setUp() {
        // protected 생성자를 가진 User 클래스 모킹
        testUser = Mockito.mock(User.class);
        when(testUser.getId()).thenReturn(1L);

        testToken = FCMToken.builder()
                .user(testUser)
                .token("test-fcm-token")
                .build();
        try {
            java.lang.reflect.Field idField = FCMToken.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testToken, 1L);

            // active 필드를 직접 설정하지 않아도 기본값이 true임
        } catch (Exception e) {
            // 예외 무시
        }

        testEvent = new NotificationEvent() {
            @Override
            public Long getRecipientId() {
                return 1L;
            }

            @Override
            public String getTitle() {
                return "테스트 알림";
            }

            @Override
            public String getContent() {
                return "테스트 내용";
            }

            @Override
            public NotificationType getType() {
                return NotificationType.MATCHING_ACCEPTED;
            }

            @Override
            public Long getRelatedEntityId() {
                return 2L;
            }
        };
    }

    @Test
    @DisplayName("FCM 토큰 등록 성공")
    void registerToken_shouldRegisterNewToken() {
        // given
        FCMTokenRequest request = new FCMTokenRequest("new-fcm-token");
        when(userService.findUserById(anyLong())).thenReturn(testUser);
        when(fcmTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // when
        FCMTokenResponse response = fcmService.registerToken(1L, request);

        // then
        verify(fcmTokenRepository).save(any(FCMToken.class));
        assertTrue(response.isSuccess());
        assertEquals("FCM 토큰이 성공적으로 등록되었습니다.", response.getMessage());
    }

    @Test
    @DisplayName("이미 존재하는 토큰을 다른 사용자가 등록하는 경우")
    void registerToken_shouldDeactivateExistingAndCreateNew() {
        // given
        FCMTokenRequest request = new FCMTokenRequest("existing-token");
        User otherUser = Mockito.mock(User.class);
        when(otherUser.getId()).thenReturn(2L);

        FCMToken existingToken = FCMToken.builder()
                .user(otherUser)
                .token("existing-token")
                .build();

        when(userService.findUserById(eq(1L))).thenReturn(testUser);
        when(fcmTokenRepository.findByToken(eq("existing-token"))).thenReturn(Optional.of(existingToken));

        // when
        FCMTokenResponse response = fcmService.registerToken(1L, request);

        // then
        verify(fcmTokenRepository).save(any(FCMToken.class));
        assertFalse(existingToken.isActive());
        assertTrue(response.isSuccess());
    }

    @Test
    @DisplayName("FCM 알림 전송 성공")
    void sendNotification_shouldSendToAllActiveTokens() throws FirebaseMessagingException {
        // given
        List<FCMToken> tokens = Arrays.asList(testToken);
        when(fcmTokenRepository.findByUserIdAndActiveIsTrue(eq(1L))).thenReturn(tokens);
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        // when
        fcmService.sendNotification(1L, testEvent);

        // then
        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    @DisplayName("활성 토큰이 없는 경우 FCM 발송 안함")
    void sendNotification_shouldNotSendWhenNoActiveTokens() throws FirebaseMessagingException {
        // given
        when(fcmTokenRepository.findByUserIdAndActiveIsTrue(eq(1L))).thenReturn(Arrays.asList());

        // when
        fcmService.sendNotification(1L, testEvent);

        // then
        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    @DisplayName("FCM 전송 중 토큰 무효 오류 처리")
    void sendNotification_shouldDeactivateInvalidToken() throws FirebaseMessagingException {
        // given
        List<FCMToken> tokens = Arrays.asList(testToken);
        when(fcmTokenRepository.findByUserIdAndActiveIsTrue(eq(1L))).thenReturn(tokens);

        FirebaseMessagingException exception = Mockito.mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

        // when
        fcmService.sendNotification(1L, testEvent);

        // then
        assertFalse(testToken.isActive());
        verify(fcmTokenRepository).save(eq(testToken));
    }

    @Test
    @DisplayName("FCM 토큰 비활성화 성공")
    void deactivateToken_shouldDeactivateToken() throws FirebaseMessagingException {
        // given
        FCMTokenRequest request = new FCMTokenRequest("test-fcm-token");

        when(fcmTokenRepository.findByToken(eq("test-fcm-token"))).thenReturn(Optional.of(testToken));
        when(testUser.getId()).thenReturn(1L);

        // when
        FCMTokenResponse response = fcmService.deactivateToken(1L, request);

        // then
        assertTrue(response.isSuccess());
        assertEquals("FCM 토큰이 비활성화되었습니다.", response.getMessage());

        ArgumentCaptor<FCMToken> tokenCaptor = ArgumentCaptor.forClass(FCMToken.class);
        verify(fcmTokenRepository, never()).save(tokenCaptor.capture());

        assertFalse(testToken.isActive());
    }

    @Test
    @DisplayName("FCM 토큰 비활성화 실패 - 토큰 찾을 수 없음")
    void deactivateToken_shouldThrowExceptionWhenTokenNotFound() throws FirebaseMessagingException {
        // given
        FCMTokenRequest request = new FCMTokenRequest("non-existent-token");

        when(fcmTokenRepository.findByToken(eq("non-existent-token"))).thenReturn(Optional.empty());

        // then
        assertThrows(CustomException.class, () -> {
            // when
            fcmService.deactivateToken(1L, request);
        });
    }

    @Test
    @DisplayName("FCM 토큰 비활성화 실패 - 권한 없음")
    void deactivateToken_shouldThrowExceptionWhenUnauthorized() throws FirebaseMessagingException {
        // given
        FCMTokenRequest request = new FCMTokenRequest("test-fcm-token");

        when(fcmTokenRepository.findByToken(eq("test-fcm-token"))).thenReturn(Optional.of(testToken));
        when(testUser.getId()).thenReturn(1L);

        // then
        assertThrows(CustomException.class, () -> {
            fcmService.deactivateToken(2L, request);
        });
    }

    @Test
    @DisplayName("사용자의 모든 FCM 토큰 비활성화")
    void deactivateAllTokens_shouldDeactivateAllUserTokens() throws FirebaseMessagingException {
        // given
        Long userId = 1L;
        FCMToken token1 = FCMToken.builder().user(testUser).token("token1").build();
        FCMToken token2 = FCMToken.builder().user(testUser).token("token2").build();
        List<FCMToken> activeTokens = Arrays.asList(token1, token2);

        when(userService.findUserById(eq(userId))).thenReturn(testUser);
        when(fcmTokenRepository.findByUserIdAndActiveIsTrue(eq(userId))).thenReturn(activeTokens);

        // when
        fcmService.deactivateAllTokens(userId);

        // then
        assertFalse(token1.isActive());
        assertFalse(token2.isActive());

        verify(fcmTokenRepository).saveAll(eq(activeTokens));
    }

    @Test
    @DisplayName("활성 토큰이 없는 사용자의 토큰 비활성화 시도")
    void deactivateAllTokens_shouldHandleNoActiveTokens() throws FirebaseMessagingException {
        // given
        Long userId = 1L;

        when(userService.findUserById(eq(userId))).thenReturn(testUser);
        when(fcmTokenRepository.findByUserIdAndActiveIsTrue(eq(userId))).thenReturn(Arrays.asList());

        // when
        fcmService.deactivateAllTokens(userId);

        // then
        verify(fcmTokenRepository, never()).saveAll(anyList());
    }
}