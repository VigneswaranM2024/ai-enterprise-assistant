package com.enterprise.assistant.service.auth;

import com.enterprise.assistant.dto.request.LoginRequest;
import com.enterprise.assistant.dto.request.RefreshTokenRequest;
import com.enterprise.assistant.dto.request.RegisterRequest;
import com.enterprise.assistant.dto.response.AuthenticationResponse;
import com.enterprise.assistant.dto.response.UserDTO;

import java.util.UUID;

/**
 * Service Contract for Authentication Operations (Login, Register, Token Refresh, User Profile).
 */
public interface AuthenticationService {

    AuthenticationResponse login(LoginRequest request);

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    UserDTO getCurrentUserProfile(UUID userId);
}
