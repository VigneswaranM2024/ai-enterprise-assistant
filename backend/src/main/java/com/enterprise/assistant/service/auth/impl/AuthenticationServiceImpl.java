package com.enterprise.assistant.service.auth.impl;

import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.domain.user.Role;
import com.enterprise.assistant.domain.user.SecurityClassification;
import com.enterprise.assistant.domain.user.User;
import com.enterprise.assistant.dto.request.LoginRequest;
import com.enterprise.assistant.dto.request.RefreshTokenRequest;
import com.enterprise.assistant.dto.request.RegisterRequest;
import com.enterprise.assistant.dto.response.AuthenticationResponse;
import com.enterprise.assistant.dto.response.UserDTO;
import com.enterprise.assistant.exception.AuthException;
import com.enterprise.assistant.exception.ResourceNotFoundException;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.repository.user.RoleRepository;
import com.enterprise.assistant.repository.user.UserRepository;
import com.enterprise.assistant.security.jwt.JwtTokenProvider;
import com.enterprise.assistant.security.user.CustomUserDetailsService;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Transactional Implementation of Authentication Service.
 * High-performance, unit-testable service handling user authentication lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        log.info("Attempting authentication for email: {} in tenant: {}", request.email(), request.tenantSlug());

        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with slug: " + request.tenantSlug()));

        User user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.email())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new AuthException("User account is disabled");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        UserDTO profile = mapToUserDTO(user);

        log.info("Successfully authenticated user: {} (ID: {})", user.getEmail(), user.getId());
        return new AuthenticationResponse(accessToken, refreshToken, tokenProvider.getJwtExpirationInSeconds(), profile);
    }

    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Attempting registration for email: {} in tenant: {}", request.email(), request.tenantSlug());

        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .slug(request.tenantSlug())
                        .name(request.tenantSlug().toUpperCase().replace("-", " "))
                        .status("ACTIVE")
                        .build()));

        if (userRepository.existsByTenantIdAndEmail(tenant.getId(), request.email())) {
            throw new AuthException("Email address is already registered in this tenant");
        }

        Role defaultRole = roleRepository.findByTenantIdAndName(tenant.getId(), "ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .tenant(tenant)
                        .name("ROLE_EMPLOYEE")
                        .description("Default Employee Role")
                        .isSystemRole(true)
                        .build()));

        User newUser = User.builder()
                .tenant(tenant)
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .jobTitle(request.jobTitle())
                .securityClassification(SecurityClassification.INTERNAL)
                .isActive(true)
                .roles(Set.of(defaultRole))
                .build();

        User savedUser = userRepository.save(newUser);
        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        UserDTO profile = mapToUserDTO(savedUser);

        log.info("Successfully registered new user: {} (ID: {})", savedUser.getEmail(), savedUser.getId());
        return new AuthenticationResponse(accessToken, refreshToken, tokenProvider.getJwtExpirationInSeconds(), profile);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromJWT(refreshToken);
        UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserById(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDTO profile = mapToUserDTO(user);

        return new AuthenticationResponse(newAccessToken, newRefreshToken, tokenProvider.getJwtExpirationInSeconds(), profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return mapToUserDTO(user);
    }

    private UserDTO mapToUserDTO(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserDTO(
                user.getId(),
                user.getTenant().getId(),
                user.getTenant().getSlug(),
                user.getEmail(),
                user.getFullName(),
                user.getJobTitle(),
                user.getSecurityClassification().name(),
                roles
        );
    }
}
