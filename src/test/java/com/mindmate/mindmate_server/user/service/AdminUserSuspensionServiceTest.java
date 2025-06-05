package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.global.util.RedisKeyManager;
import com.mindmate.mindmate_server.global.util.SlackNotifier;
import com.mindmate.mindmate_server.report.dto.UnsuspendRequest;
import com.mindmate.mindmate_server.user.domain.Profile;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.SuspendedUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserSuspensionServiceTest {

    @Mock private UserService userService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private RedisKeyManager redisKeyManager;
    @Mock private SlackNotifier slackNotifier;

    @InjectMocks
    private AdminUserSuspensionService adminUserSuspensionService;

    private User mockUser;
    private User mockAdminUser;
    private Profile mockProfile;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_ADMIN_ID = 2L;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_SUSPENSION_KEY = "user:suspension:1";
    private final String TEST_REASON = "부적절한 행동";
    private final String TEST_NICKNAME = "testUser";

    @BeforeEach
    void setUp() {
        setupMockObjects();
        setupMockBehaviors();
        setupRedisOperations();
    }

    private void setupMockObjects() {
        mockUser = mock(User.class);
        mockAdminUser = mock(User.class);
        mockProfile = mock(Profile.class);
    }

    private void setupMockBehaviors() {
        when(mockUser.getId()).thenReturn(TEST_USER_ID);
        when(mockUser.getEmail()).thenReturn(TEST_EMAIL);
        when(mockUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);
        when(mockUser.getProfile()).thenReturn(mockProfile);
        when(mockUser.getReportCount()).thenReturn(0);

        when(mockAdminUser.getId()).thenReturn(TEST_ADMIN_ID);
        when(mockAdminUser.getCurrentRole()).thenReturn(RoleType.ROLE_ADMIN);

        when(mockProfile.getNickname()).thenReturn(TEST_NICKNAME);

        when(userService.findUserById(TEST_USER_ID)).thenReturn(mockUser);
        when(userService.findUserById(TEST_ADMIN_ID)).thenReturn(mockAdminUser);
    }

    private void setupRedisOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisKeyManager.getUserSuspensionKey(TEST_USER_ID)).thenReturn(TEST_SUSPENSION_KEY);
    }

    @Nested
    @DisplayName("suspendUser 테스트")
    class SuspendUserTest {

        @Test
        @DisplayName("일반 사용자 정지 성공 - 일 단위")
        void suspendUser_Success_Days() {
            // given
            Duration duration = Duration.ofDays(7);
            int reportCount = 3;

            // when
            adminUserSuspensionService.suspendUser(TEST_USER_ID, reportCount, duration, TEST_REASON);

            // then
            verify(userService).findUserById(TEST_USER_ID);
            verify(userService).save(mockUser);
            verify(valueOperations).set(TEST_SUSPENSION_KEY, "suspended");
            verify(redisTemplate).expire(TEST_SUSPENSION_KEY, 7L, TimeUnit.DAYS);
            verify(slackNotifier).sendSuspensionAlert(mockUser, TEST_REASON, duration);
            verify(mockUser).suspend(duration);
            verify(mockUser).setReportCount(reportCount);
        }

        @Test
        @DisplayName("일반 사용자 정지 성공 - 시간 단위")
        void suspendUser_Success_Hours() {
            // given
            Duration duration = Duration.ofHours(12);
            int reportCount = 2;

            // when
            adminUserSuspensionService.suspendUser(TEST_USER_ID, reportCount, duration, TEST_REASON);

            // then
            verify(redisTemplate).expire(TEST_SUSPENSION_KEY, 12L, TimeUnit.HOURS);
            verify(mockUser).suspend(duration);
        }

        @Test
        @DisplayName("관리자 정지 시도 시 예외 발생")
        void suspendUser_AdminUser_ThrowsException() {
            // given
            Duration duration = Duration.ofDays(1);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminUserSuspensionService.suspendUser(TEST_ADMIN_ID, 1, duration, TEST_REASON));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.ADMIN_SUSPENSION_NOT_ALLOWED);
            verify(userService, never()).save(any());
            verify(valueOperations, never()).set(anyString(), any());
        }

        @Test
        @DisplayName("음수 신고 횟수로 정지 시 신고 횟수 업데이트 안함")
        void suspendUser_NegativeReportCount_DoesNotUpdate() {
            // given
            Duration duration = Duration.ofDays(1);

            // when
            adminUserSuspensionService.suspendUser(TEST_USER_ID, -1, duration, TEST_REASON);

            // then
            verify(mockUser, never()).setReportCount(anyInt());
            verify(userService).save(mockUser);
        }
    }

    @Nested
    @DisplayName("unsuspendUser 테스트")
    class UnsuspendUserTest {

        @Test
        @DisplayName("정지된 사용자 정지 해제 성공")
        void unsuspendUser_Success() {
            // given
            UnsuspendRequest request = new UnsuspendRequest();
            request.setReportCount(1);

            when(mockUser.getCurrentRole()).thenReturn(RoleType.ROLE_SUSPENDED);

            // when
            adminUserSuspensionService.unsuspendUser(TEST_USER_ID, request);

            // then
            verify(userService).findUserById(TEST_USER_ID);
            verify(userService).save(mockUser);
            verify(redisTemplate).delete(TEST_SUSPENSION_KEY);
            verify(mockUser).setReportCount(1);
            verify(mockUser).unsuspend();
        }

        @Test
        @DisplayName("이미 정지되지 않은 사용자 정지 해제 시도 시 예외 발생")
        void unsuspendUser_AlreadyNotSuspended_ThrowsException() {
            // given
            UnsuspendRequest request = new UnsuspendRequest();
            when(mockUser.getCurrentRole()).thenReturn(RoleType.ROLE_USER);

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> adminUserSuspensionService.unsuspendUser(TEST_USER_ID, request));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_ALREADY_NOT_SUSPENDED);
            verify(userService, never()).save(any());
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("getAllSuspendedUsers 테스트")
    class GetAllSuspendedUsersTest {

        @Test
        @DisplayName("정지된 사용자 목록 조회 성공")
        void getAllSuspendedUsers_Success() {
            // given
            Set<String> suspensionKeys = Set.of("user:suspension:1", "user:suspension:2");
            LocalDateTime suspensionEndTime = LocalDateTime.now().plusDays(7);

            when(mockUser.getSuspensionEndTime()).thenReturn(suspensionEndTime);
            when(mockUser.getReportCount()).thenReturn(3);

            when(redisTemplate.keys("user:suspension:*")).thenReturn(suspensionKeys);
            when(userService.findUserById(1L)).thenReturn(mockUser);
            when(userService.findUserById(2L)).thenReturn(mockUser);
            when(valueOperations.get("user:suspension:1")).thenReturn("부적절한 행동");
            when(valueOperations.get("user:suspension:2")).thenReturn("스팸");

            // when
            List<SuspendedUserDTO> result = adminUserSuspensionService.getAllSuspendedUsers();

            // then
            assertThat(result).hasSize(2);
            verify(redisTemplate).keys("user:suspension:*");
            verify(userService, times(2)).findUserById(anyLong());
        }

        @Test
        @DisplayName("Redis에서 정지 사유를 가져올 수 없는 경우 Unknown으로 설정")
        void getAllSuspendedUsers_UnknownReason() {
            // given
            Set<String> suspensionKeys = Set.of("user:suspension:1");

            when(redisTemplate.keys("user:suspension:*")).thenReturn(suspensionKeys);
            when(userService.findUserById(1L)).thenReturn(mockUser);
            when(valueOperations.get("user:suspension:1")).thenReturn(null);

            // when
            List<SuspendedUserDTO> result = adminUserSuspensionService.getAllSuspendedUsers();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSuspensionReason()).isEqualTo("Unknown");
        }
    }
}
