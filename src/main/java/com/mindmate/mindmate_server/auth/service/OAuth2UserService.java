package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.domain.AuthProvider;
import com.mindmate.mindmate_server.auth.dto.GoogleOAuth2UserInfo;
import com.mindmate.mindmate_server.chat.domain.UserPrincipal;
import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(e.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());

        if (StringUtils.isEmpty(userInfo.getEmail())) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_FOUND);
        }

        Optional<User> userOptional = userService.findByEmailOptional(userInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            log.info("기존 사용자 발견: {}", userInfo.getEmail());
            user = userOptional.get();
        } else {
            log.info("새 사용자 등록: {}", userInfo.getEmail());
            user = registerNewUser(userRequest, userInfo);
        }
        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest userRequest, GoogleOAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.getEmail())
                .provider(AuthProvider.GOOGLE)
                .providerId(userInfo.getId())
                .role(RoleType.ROLE_USER)
                .build();

        return userService.save(user);
    }
}
