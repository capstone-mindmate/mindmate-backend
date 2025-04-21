package com.mindmate.mindmate_server.auth.util;

import com.mindmate.mindmate_server.global.exception.AuthErrorCode;
import com.mindmate.mindmate_server.global.exception.CustomException;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {
    /**
     * 길이: 8 ~ 20
     * 대문자, 소문자, 숫자, 특수문자 반드시 포함
     */
    public void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 20) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (!password.matches(".*[a-z].*")) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (!password.matches(".*[0-9].*")) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (!password.matches(".*[!@#$%^&*()].*")) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD_FORMAT);
        }
    }

    public void validatePasswordMatch(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
        }
    }
}
