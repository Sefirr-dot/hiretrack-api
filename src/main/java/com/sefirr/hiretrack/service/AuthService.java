package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.request.LoginRequest;
import com.sefirr.hiretrack.dto.request.RegisterRequest;
import com.sefirr.hiretrack.dto.response.AuthResponse;
import com.sefirr.hiretrack.entity.RefreshToken;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.Role;
import com.sefirr.hiretrack.exception.ConflictException;
import com.sefirr.hiretrack.exception.InvalidTokenException;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already in use: " + email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.USER)
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        // Throws BadCredentialsException (-> 401) on wrong email/password
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Invalid email or password"));

        return issueTokens(user);
    }

    /**
     * Rotates the refresh token: the presented token is revoked and a new pair is issued.
     */
    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(hash(refreshTokenValue))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        refreshTokenRepository.delete(stored);

        if (stored.isExpired()) {
            throw new InvalidTokenException("Refresh token expired. Please log in again.");
        }

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.deleteByToken(hash(refreshTokenValue));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken, user.getEmail(), user.getFullName());
    }

    private String createRefreshToken(User user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Only the hash is persisted, so a database leak does not expose usable tokens
        refreshTokenRepository.save(RefreshToken.builder()
                .token(hash(tokenValue))
                .user(user)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .build());

        return tokenValue;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
