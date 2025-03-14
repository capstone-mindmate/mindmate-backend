package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.dto.LoginRequest;
import com.mindmate.mindmate_server.auth.dto.LoginResponse;
import com.mindmate.mindmate_server.auth.dto.SignUpRequest;
import com.mindmate.mindmate_server.auth.dto.TokenResponse;

public interface AuthService {

    void registerUser(SignUpRequest request);

    void verifyEmail(String token);

    LoginResponse login(LoginRequest request);

    void logout(String bearer);

    TokenResponse refresh(String bearer, String bearer1);

    void resendVerificationEmail(String email);
}
