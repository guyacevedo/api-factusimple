package com.factusimple.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.infrastructure.config.AppProperties.Jwt;
import com.factusimple.infrastructure.config.AppProperties.Security;
import com.factusimple.user.entity.Role;
import com.factusimple.user.entity.User;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        Jwt jwt = new Jwt("clave-de-prueba-minimo-32-caracteres-larga-ok",
                Duration.ofHours(24), Duration.ofDays(30));
        AppProperties props = new AppProperties(new Security(jwt, null), null, null);
        jwtService = new JwtService(props);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("u@test.local");
        user.setRole(Role.USER);
    }

    @Test
    void access_token_roundtrip_contains_subject_and_type() {
        // Act
        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parse(token);

        // Assert
        assertThat(jwtService.extractUserId(claims)).isEqualTo(user.getId());
        assertThat(jwtService.extractType(claims)).isEqualTo("ACCESS");
    }

    @Test
    void refresh_and_activation_tokens_have_expected_types() {
        // Act + Assert
        assertThat(jwtService.extractType(jwtService.parse(jwtService.generateRefreshToken(user))))
                .isEqualTo("REFRESH");
        assertThat(jwtService.extractType(jwtService.parse(jwtService.generateActivationToken(user))))
                .isEqualTo("ACTIVATION");
    }

    @Test
    void two_tokens_are_unique_due_to_jti() {
        // Act
        String a = jwtService.generateAccessToken(user);
        String b = jwtService.generateAccessToken(user);

        // Assert: el jti único evita colisiones (constraint unique de tokens.token).
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void parsing_tampered_token_fails() {
        // Arrange
        String token = jwtService.generateAccessToken(user) + "x";

        // Act + Assert
        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(Exception.class);
    }
}
