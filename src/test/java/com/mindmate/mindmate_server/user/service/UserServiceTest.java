package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.dto.PushNotificationSettingRequest;
import com.mindmate.mindmate_server.user.dto.PushNotificationSettingResponse;
import com.mindmate.mindmate_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;
    private final Long TEST_USER_ID = 1L;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email(TEST_EMAIL)
                .provider(AuthProvider.GOOGLE)
                .providerId("google123")
                .role(RoleType.ROLE_USER)
                .build();

        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(mockUser, TEST_USER_ID);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID for test", e);
        }
    }

    @Nested
    @DisplayName("findUserById 테스트")
    class FindUserByIdTest {

        @Test
        @DisplayName("정상적으로 사용자를 찾는 경우")
        void findUserById_Success() {
            // given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));

            // when
            User result = userService.findUserById(TEST_USER_ID);

            // then
            assertThat(result).isEqualTo(mockUser);
            verify(userRepository).findById(TEST_USER_ID);
        }

        @Test
        @DisplayName("사용자를 찾지 못한 경우 예외 발생")
        void findUserById_UserNotFound_ThrowsException() {
            // given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.findUserById(TEST_USER_ID));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(userRepository).findById(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("findByEmail 테스트")
    class FindByEmailTest {

        @Test
        @DisplayName("정상적으로 이메일로 사용자를 찾는 경우")
        void findByEmail_Success() {
            // given
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));

            // when
            User result = userService.findByEmail(TEST_EMAIL);

            // then
            assertThat(result).isEqualTo(mockUser);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("이메일로 사용자를 찾지 못한 경우 예외 발생")
        void findByEmail_UserNotFound_ThrowsException() {
            // given
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // when & then
            CustomException exception = assertThrows(CustomException.class,
                    () -> userService.findByEmail(TEST_EMAIL));

            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @ParameterizedTest(name = "[{index}] 이메일: {0}")
        @ValueSource(strings = {"test@example.com", "user@domain.co.kr", "admin@test.org"})
        @DisplayName("다양한 이메일 형식으로 사용자 조회")
        void findByEmail_VariousEmailFormats(String email) {
            // given
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

            // when
            User result = userService.findByEmail(email);

            // then
            assertThat(result).isEqualTo(mockUser);
            verify(userRepository).findByEmail(email);
        }
    }

    @Nested
    @DisplayName("existsByEmail 테스트")
    class ExistsByEmailTest {

        @ParameterizedTest(name = "[{index}] 이메일 존재 여부: {0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("이메일 존재 여부 확인")
        void existsByEmail_ParamTest(boolean exists) {
            // given
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(exists);

            // when
            boolean result = userService.existsByEmail(TEST_EMAIL);

            // then
            assertThat(result).isEqualTo(exists);
            verify(userRepository).existsByEmail(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("save 테스트")
    class SaveTest {

        @Test
        @DisplayName("사용자 저장 성공")
        void save_Success() {
            // given
            User savedUser = User.builder()
                    .email("saved@example.com")
                    .provider(AuthProvider.GOOGLE)
                    .providerId("google456")
                    .role(RoleType.ROLE_USER)
                    .build();

            when(userRepository.save(mockUser)).thenReturn(savedUser);

            // when
            User result = userService.save(mockUser);

            // then
            assertThat(result).isEqualTo(savedUser);
            verify(userRepository).save(mockUser);
        }
    }

    @Nested
    @DisplayName("findByCurrentRoleAndSuspensionEndTimeBefore 테스트")
    class FindByCurrentRoleAndSuspensionEndTimeBeforeTest {

        @Test
        @DisplayName("역할과 정지 종료 시간으로 사용자 목록 조회")
        void findByCurrentRoleAndSuspensionEndTimeBefore_Success() {
            // given
            RoleType roleType = RoleType.ROLE_SUSPENDED;
            LocalDateTime time = LocalDateTime.now();
            List<User> expectedUsers = Arrays.asList(mockUser);

            when(userRepository.findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time))
                    .thenReturn(expectedUsers);

            // when
            List<User> result = userService.findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time);

            // then
            assertThat(result).isEqualTo(expectedUsers);
            verify(userRepository).findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time);
        }

        @Test
        @DisplayName("조건에 맞는 사용자가 없는 경우 빈 리스트 반환")
        void findByCurrentRoleAndSuspensionEndTimeBefore_EmptyList() {
            // given
            RoleType roleType = RoleType.ROLE_SUSPENDED;
            LocalDateTime time = LocalDateTime.now();
            List<User> emptyList = Collections.emptyList();

            when(userRepository.findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time))
                    .thenReturn(emptyList);

            // when
            List<User> result = userService.findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time);

            // then
            assertThat(result).isEmpty();
            verify(userRepository).findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time);
        }
    }

    @Nested
    @DisplayName("findAllUserIds 테스트")
    class FindAllUserIdsTest {

        @Test
        @DisplayName("모든 사용자 ID 조회 성공")
        void findAllUserIds_Success() {
            // given
            List<Long> expectedIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
            when(userRepository.findAllUserIds()).thenReturn(expectedIds);

            // when
            List<Long> result = userService.findAllUserIds();

            // then
            assertThat(result).isEqualTo(expectedIds);
            assertThat(result).hasSize(5);
            verify(userRepository).findAllUserIds();
        }

        @Test
        @DisplayName("사용자가 없는 경우 빈 리스트 반환")
        void findAllUserIds_EmptyList() {
            // given
            List<Long> emptyList = Collections.emptyList();
            when(userRepository.findAllUserIds()).thenReturn(emptyList);

            // when
            List<Long> result = userService.findAllUserIds();

            // then
            assertThat(result).isEmpty();
            verify(userRepository).findAllUserIds();
        }
    }

    @Nested
    @DisplayName("findByEmailOptional 테스트")
    class FindByEmailOptionalTest {

        @Test
        @DisplayName("이메일로 사용자를 찾은 경우 Optional 반환")
        void findByEmailOptional_UserFound() {
            // given
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));

            // when
            Optional<User> result = userService.findByEmailOptional(TEST_EMAIL);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(mockUser);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("이메일로 사용자를 찾지 못한 경우 빈 Optional 반환")
        void findByEmailOptional_UserNotFound() {
            // given
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

            // when
            Optional<User> result = userService.findByEmailOptional(TEST_EMAIL);

            // then
            assertThat(result).isEmpty();
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @ParameterizedTest(name = "[{index}] 이메일: {0}, 존재 여부: {1}")
        @MethodSource("emailOptionalScenarios")
        @DisplayName("다양한 이메일 시나리오로 Optional 조회")
        void findByEmailOptional_ParamTest(String email, boolean userExists) {
            // given
            Optional<User> expectedResult = userExists ? Optional.of(mockUser) : Optional.empty();
            when(userRepository.findByEmail(email)).thenReturn(expectedResult);

            // when
            Optional<User> result = userService.findByEmailOptional(email);

            // then
            assertThat(result).isEqualTo(expectedResult);
            verify(userRepository).findByEmail(email);
        }

        static Stream<Arguments> emailOptionalScenarios() {
            return Stream.of(
                    Arguments.of("existing@example.com", true),
                    Arguments.of("nonexistent@example.com", false),
                    Arguments.of("test@domain.co.kr", true),
                    Arguments.of("invalid@test.org", false)
            );
        }
    }

    @Nested
    @DisplayName("getPushNotificationSetting 테스트")
    class GetPushNotificationSettingTest {

        @Test
        @DisplayName("푸시 알림 설정 조회 성공")
        void getPushNotificationSetting_Success() {
            // given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));

            // when
            PushNotificationSettingResponse result = userService.getPushNotificationSetting(TEST_USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(userRepository).findById(TEST_USER_ID);
        }

    }

    @Nested
    @DisplayName("updatePushNotificationSetting 테스트")
    class UpdatePushNotificationSettingTest {

        @Test
        @DisplayName("푸시 알림 설정 업데이트 성공 - 활성화")
        void updatePushNotificationSetting_EnableSuccess() {
            // given
            PushNotificationSettingRequest request = PushNotificationSettingRequest.builder()
                    .pushNotificationEnabled(true)
                    .build();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(mockUser)).thenReturn(mockUser);

            // when
            PushNotificationSettingResponse result = userService.updatePushNotificationSetting(TEST_USER_ID, request);

            // then
            assertThat(result).isNotNull();
            verify(userRepository).findById(TEST_USER_ID);
            verify(userRepository).save(mockUser);
        }

        @Test
        @DisplayName("푸시 알림 설정 업데이트 성공 - 비활성화")
        void updatePushNotificationSetting_DisableSuccess() {
            // given
            PushNotificationSettingRequest request = PushNotificationSettingRequest.builder()
                    .pushNotificationEnabled(false)
                    .build();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));
            when(userRepository.save(mockUser)).thenReturn(mockUser);

            // when
            PushNotificationSettingResponse result = userService.updatePushNotificationSetting(TEST_USER_ID, request);

            // then
            assertThat(result).isNotNull();
            verify(userRepository).findById(TEST_USER_ID);
            verify(userRepository).save(mockUser);
        }

    }

    @Nested
    @DisplayName("isPushNotificationEnabled 테스트")
    class IsPushNotificationEnabledTest {

        @Test
        @DisplayName("푸시 알림이 활성화된 사용자의 상태 확인")
        void isPushNotificationEnabled_EnabledUser() {
            // given
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(mockUser));

            // when
            boolean result = userService.isPushNotificationEnabled(TEST_USER_ID);

            // then
            assertThat(result).isTrue();
            verify(userRepository).findById(TEST_USER_ID);
        }
    }
}
