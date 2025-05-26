package com.mindmate.mindmate_server.auth.service;

import com.mindmate.mindmate_server.auth.dto.TokenResponse;

public interface AuthService {

    void logout(String bearer);

    TokenResponse refresh(String bearer, String bearer1);
}
