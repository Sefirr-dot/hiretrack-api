package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.request.LoginRequest;
import com.sefirr.hiretrack.dto.request.RegisterRequest;
import com.sefirr.hiretrack.dto.response.AuthResponse;
import com.sefirr.hiretrack.entity.RefreshToken;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.Role;
import com.sefirr.hiretrack.exception.ResourceNotFoundException;
import com.sefirr.hiretrack.exception.UnauthorizedException;
import com.sefirr.hiretrack.repository.RefreshTokenRepository;
import com.sefirr.hiretrack.repository.UserRepository;
import com.sefirr.hiretrack.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = saveRefreshToken(user);

        return AuthResponse.of(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenRepository.deleteByUserId(user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = saveRefreshToken(user);

        return AuthResponse.of(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired. Please log in again.");
        }

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.of(newAccessToken, refreshTokenValue, user.getEmail(), user.getFullName());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    private String saveRefreshToken(User user) {
        String tokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }
}
