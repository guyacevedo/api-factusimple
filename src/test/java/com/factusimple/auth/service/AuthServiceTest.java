package com.factusimple.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.factusimple.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.factusimple.auth.dto.AuthDtos.LoginRequest;
import com.factusimple.auth.dto.AuthDtos.ResetPasswordRequest;
import com.factusimple.auth.entity.PasswordResetToken;
import com.factusimple.auth.repository.PasswordResetTokenRepository;
import com.factusimple.auth.repository.TokenRepository;
import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.infrastructure.config.AppProperties.Lockout;
import com.factusimple.infrastructure.exception.AppException;
import com.factusimple.infrastructure.security.JwtService;
import com.factusimple.plan.repository.PlanRepository;
import com.factusimple.user.entity.User;
import com.factusimple.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PlanRepository planRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthTokenService authTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null,
                new Lockout(3, Duration.ofMinutes(15)));
        authService = new AuthService(userRepository, planRepository, resetTokenRepository,
                tokenRepository, passwordEncoder, jwtService, authTokenService, props);
    }

    private User enabledUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("u@test.local");
        u.setPassword("hash");
        u.setEnabled(true);
        return u;
    }

    @Test
    void login_with_unknown_email_throws_invalid_credentials_uniformly() {
        // Arrange
        when(userRepository.findByEmail("x@test.local")).thenReturn(Optional.empty());

        // Act + Assert: no revela si el correo existe.
        assertThatThrownBy(() -> authService.login(new LoginRequest("x@test.local", "pw")))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_CREDENTIALS");
    }

    @Test
    void login_locks_account_after_max_failed_attempts() {
        // Arrange: ya lleva 2 intentos fallidos, este sería el tercero (máx=3).
        User user = enabledUser();
        user.setFailedLoginAttempts(2);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        // Act
        assertThatThrownBy(() -> authService.login(new LoginRequest("u@test.local", "bad")))
                .hasFieldOrPropertyWithValue("errorCode", "INVALID_CREDENTIALS");

        // Assert: se bloqueó la cuenta.
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLockedUntil()).isNotNull();
    }

    @Test
    void login_with_unactivated_account_throws() {
        // Arrange
        User user = enabledUser();
        user.setEnabled(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "hash")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest("u@test.local", "good")))
                .hasFieldOrPropertyWithValue("errorCode", "ACCOUNT_NOT_ACTIVATED");
    }

    @Test
    void forgot_password_for_unknown_email_does_not_persist_token() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        authService.forgotPassword(new ForgotPasswordRequest("ghost@test.local"));

        // Assert: respuesta uniforme, sin crear token.
        verify(resetTokenRepository, never()).save(any());
    }

    @Test
    void forgot_password_stores_hashed_token_not_raw() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(enabledUser()));

        // Act
        authService.forgotPassword(new ForgotPasswordRequest("u@test.local"));

        // Assert: se guarda un hash SHA-256 (64 hex), no el token en claro.
        ArgumentCaptor<PasswordResetToken> captor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void reset_password_with_expired_token_throws() {
        // Arrange
        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(enabledUser());
        prt.setExpiresAt(Instant.now().minusSeconds(60));
        when(resetTokenRepository.findByTokenHashAndUsedFalse(anyString()))
                .thenReturn(Optional.of(prt));

        // Act + Assert
        assertThatThrownBy(() ->
                authService.resetPassword(new ResetPasswordRequest("rawtoken", "NewPass99")))
                .hasFieldOrPropertyWithValue("errorCode", "TOKEN_EXPIRED");
    }
}
