package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuth2UserServiceTest {
    @Mock private UserService userService;
    @Mock private OAuth2UserRequest userRequest;
    @Mock private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2UserService oAuth2UserService;

    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_ID = "google-user-id-123";
    private static final String USER_NAME = "Test User";
    private static final AuthProvider PROVIDER = AuthProvider.GOOGLE;
    private static final RoleType DEFAULT_ROLE = RoleType.ROLE_USER;

    private Map<String, Object> attributes;
    private User existingUser;
    private User newUser;

    @BeforeEach
    void setup() {
        attributes = createGoogleAttributes(USER_ID, USER_EMAIL, USER_NAME);
        existingUser = createUser(1L, USER_EMAIL, RoleType.ROLE_USER);
        newUser = createUser(2L, USER_EMAIL, RoleType.ROLE_USER);

        when(oAuth2User.getAttributes()).thenReturn(attributes);
    }

    private Map<String, Object> createGoogleAttributes(String id, String email, String name) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", id);
        attributes.put("email", email);
        attributes.put("name", name);
        return attributes;
    }

    private User createUser(Long id, String email, RoleType role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getEmail()).thenReturn(email);
        when(user.getCurrentRole()).thenReturn(role);
        return user;
    }

    @Test
    @DisplayName("기존 사용자 로그인 시 UserPrincipal 반환")
    void processOAuth2User_ShouldReturnUserPrincipal_WhenExistingUser() {
        // given
        when(userService.findByEmailOptional(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        // when
        OAuth2User result = oAuth2UserService.processOAuth2User(userRequest, oAuth2User);

        // then
        assertThat(result).isInstanceOf(UserPrincipal.class);
        verify(userService).findByEmailOptional(USER_EMAIL);
        verify(userService, never()).save(any(User.class));
    }

    @Test
    @DisplayName("새 사용자 등록 시 UserPrincipal 반환")
    void processOAuth2User_ShouldRegisterNewUser_WhenUserNotExists() {
        // given
        when(userService.findByEmailOptional(USER_EMAIL)).thenReturn(Optional.empty());
        when(userService.save(any(User.class))).thenReturn(newUser);

        // when
        OAuth2User result = oAuth2UserService.processOAuth2User(userRequest, oAuth2User);

        // then
        assertThat(result).isInstanceOf(UserPrincipal.class);
        verify(userService).findByEmailOptional(USER_EMAIL);
        verify(userService).save(argThat(user ->
                USER_EMAIL.equals(user.getEmail()) &&
                PROVIDER.equals(user.getProvider()) &&
                USER_ID.equals(user.getProviderId()) &&
                DEFAULT_ROLE.equals(user.getCurrentRole())
        ));
    }

    @ParameterizedTest
    @DisplayName("이메일이 없는 경우 예외 발생")
    @MethodSource("invalidEmailScenarios")
    void processOAuth2User_ShouldThrowException_WhenEmailNotFound(String email) {
        // given
        Map<String, Object> invalidAttributes = createGoogleAttributes(USER_ID, email, USER_NAME);
        when(oAuth2User.getAttributes()).thenReturn(invalidAttributes);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> oAuth2UserService.processOAuth2User(userRequest, oAuth2User));
        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_NOT_FOUND);
    }

    static Stream<Arguments> invalidEmailScenarios() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("  ")
        );
    }

}