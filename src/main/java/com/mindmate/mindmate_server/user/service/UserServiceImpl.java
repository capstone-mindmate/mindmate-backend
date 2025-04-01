package com.mindmate.mindmate_server.user.service;

import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import com.mindmate.mindmate_server.global.exception.UserErrorCode;
import com.mindmate.mindmate_server.user.domain.RoleType;
import com.mindmate.mindmate_server.user.domain.User;
import com.mindmate.mindmate_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

//    @Override
//    public boolean existsByNickname(String nickname) {
//        return userRepository.existsByNickname(nickname);
//    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public User findVerificationToken(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));
    }

    @Override
    public List<User> findByCurrentRoleAndSuspensionEndTimeBefore(RoleType roleType, LocalDateTime time) {
        return userRepository.findByCurrentRoleAndSuspensionEndTimeBefore(roleType, time);
    }
}
