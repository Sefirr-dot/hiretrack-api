package com.sefirr.hiretrack.security;

import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "dGVzdHNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3Jtb2Nrcy0xMjM0NTY3ODk=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604_800_000L);
    }

    @Test
    void generateAccessToken_isValid() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.USER)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void generateRefreshToken_hasLongerExpiry() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.USER)
                .build();

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        assertThat(access).isNotEqualTo(refresh);
        assertThat(jwtService.isTokenValid(refresh, user)).isTrue();
    }
}
