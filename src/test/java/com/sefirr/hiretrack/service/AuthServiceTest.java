package com.sefirr.hiretrack.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604_800_000L);
        user = User.builder().id(UUID.randomUUID()).email("ana@test.com").fullName("Ana").role(Role.USER).build();
    }

    @Test
    void register_normalizesEmail_andStoresOnlyTokenHash() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  Ana@Test.COM ");
        request.setPassword("password123");
        request.setFullName("Ana");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(jwtService.generateAccessToken(any())).thenReturn("access");

        AuthResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("ana@test.com");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(saved.getValue().getToken())
                .isNotEqualTo(response.getRefreshToken())
                .hasSize(64);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("ana@test.com");
        request.setPassword("password123");
        request.setFullName("Ana");
        when(userRepository.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void refresh_rotatesToken() {
        RefreshToken stored = RefreshToken.builder()
                .token("hash").user(user).expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");

        AuthResponse response = authService.refresh("old-refresh-token");

        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isNotEqualTo("old-refresh-token");
    }

    @Test
    void refresh_expiredToken_isRevokedAndRejected() {
        RefreshToken stored = RefreshToken.builder()
                .token("hash").user(user).expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("expired")).isInstanceOf(InvalidTokenException.class);
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_unknownToken_isRejected() {
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("nope")).isInstanceOf(InvalidTokenException.class);
    }
}
