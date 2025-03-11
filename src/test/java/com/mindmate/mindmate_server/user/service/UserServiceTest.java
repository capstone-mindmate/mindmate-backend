//package com.mindmate.mindmate_server.user.service;
//
//import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
//import com.mindmate.mindmate_server.global.exception.CustomException;
//import com.mindmate.mindmate_server.global.exception.UserErrorCode;
//import com.mindmate.mindmate_server.user.domain.RoleType;
//import com.mindmate.mindmate_server.user.domain.User;
//import com.mindmate.mindmate_server.user.repository.UserRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceTest {
//    @Mock private UserRepository userRepository;
//
//    @InjectMocks
//    private UserServiceImpl userService;
//
//    @Nested
//    @DisplayName("사용자 조회 테스트")
//    class FindUserTest {
//        @Test
//        @DisplayName("ID로 사용자 조회 성공")
//        void findUserById_Success() {
//            // given
//            Long userId = 1L;
//            User expectedUser = createUser();
//
//            when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
//
//            // when
//            User foundUser = userService.findUserById(userId);
//
//            // then
//            assertNotNull(foundUser);
//            assertEquals(expectedUser.getEmail(), foundUser.getEmail());
//            verify(userRepository).findById(userId);
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 ID로 조회 시 예외 발생")
//        void findUserById_UserNotFound() {
//            // given
//            Long userId = 999L;
//
//            when(userRepository.findById(userId)).thenReturn(Optional.empty());
//
//            // when & then
//            CustomException exception = assertThrows(CustomException.class, () -> userService.findUserById(userId));
//            assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
//            // then
//        }
//    }
//
//    @Nested
//    @DisplayName("이메일 관련 테스트")
//    class EmailVerificationTest {
//        @Test
//        @DisplayName("이메일로 사용자 조회 성공")
//        void findByEmail_Success() {
//            // given
//            String email = "test@example.com";
//            User expectedUser = createUser();
//
//            when(userRepository.findByEmail(email)).thenReturn(Optional.of(expectedUser));
//
//            // when
//            User foundUser = userService.findByEmail(email);
//
//            // then
//            assertNotNull(foundUser);
//            assertEquals(email, foundUser.getEmail());
//            verify(userRepository).findByEmail(email);
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 이메일로 사용자 조회 실패")
//        void findByEmail_UserNotFound() {
//            // given
//            String email = "nonexist@example.com";
//            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
//
//            // when & then
//            CustomException exception = assertThrows(CustomException.class, () -> userService.findByEmail(email));
//            assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
//        }
//
//        @Test
//        @DisplayName("이메일 존재 여부 확인 - 존재하는 경우")
//        void existsByEmail_True() {
//            // given
//            String email = "test@example.com";
//            when(userRepository.existsByEmail(email)).thenReturn(true);
//
//            // when
//            boolean exists = userService.existsByEmail(email);
//
//            // then
//            assertTrue(exists);
//            verify(userRepository).existsByEmail(email);
//        }
//
//        @Test
//        @DisplayName("토큰으로 사용자 조회 성공")
//        void findVerificationToken_Success() {
//            // given
//            String token = "valid-token";
//            User expectedUser = createUser();
//
//            when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(expectedUser));
//
//            // when
//            User foundUser = userService.findVerificationToken(token);
//
//            // then
//            assertNotNull(foundUser);
//            verify(userRepository).findByVerificationToken(token);
//        }
//
//        @Test
//        @DisplayName("잘못된 토큰으로 조히 시 예외 발생")
//        void findVerificationToken_InvalidToken() {
//            // given
//            String token = "invalid-token";
//
//            when(userRepository.findByVerificationToken(token)).thenReturn(Optional.empty());
//
//            // when & then
//            CustomException exception = assertThrows(CustomException.class, () -> userService.findVerificationToken(token));
//            assertEquals(AuthErrorCode.INVALID_TOKEN, exception.getErrorCode());
//        }
//    }
//
//    @Nested
//    @DisplayName("사용자 저장 테스트")
//    class SaveUserTest {
//        @Test
//        @DisplayName("사용자 저장 성공")
//        void save_Success() {
//            // given
//            User user = createUser();
//            when(userRepository.save(any(User.class))).thenReturn(user);
//
//            // when
//            userService.save(user);
//
//            // then
//            verify(userRepository).save(user);
//        }
//    }
//
//    private User createUser() {
//        return User.builder()
//                .email("test@example.com")
//                .password("password")
//                .role(RoleType.ROLE_USER)
//                .build();
//    }
//}