package com.sefirr.hiretrack.security;

import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "dGVzdHNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3Jtb2Nrcy0xMjM0NTY3ODk=";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = newService(SECRET, 900_000L);
        user = User.builder().id(UUID.randomUUID()).email("test@example.com").role(Role.USER).build();
    }

    private static JwtService newService(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", expirationMs);
        service.init();
        return service;
    }

    @Test
    void generateAccessToken_isValid() {
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_false_forAnotherUser() {
        String token = jwtService.generateAccessToken(user);
        User other = User.builder().email("other@example.com").role(Role.USER).build();

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void expiredToken_isRejected() {
        JwtService shortLived = newService(SECRET, -1_000L);
        String token = shortLived.generateAccessToken(user);

        assertThatThrownBy(() -> shortLived.isTokenValid(token, user)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.extractUsername(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void weakSecret_failsFastOnStartup() {
        assertThatThrownBy(() -> newService("c2hvcnQ=", 900_000L)).isInstanceOf(WeakKeyException.class);
    }
}
