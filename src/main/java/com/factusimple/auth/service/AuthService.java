package com.factusimple.auth.service;

import com.factusimple.auth.dto.AuthDtos.ActivateRequest;
import com.factusimple.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.factusimple.auth.dto.AuthDtos.LoginRequest;
import com.factusimple.auth.dto.AuthDtos.RefreshRequest;
import com.factusimple.auth.dto.AuthDtos.RegisterRequest;
import com.factusimple.auth.dto.AuthDtos.ResetPasswordRequest;
import com.factusimple.auth.dto.AuthDtos.TokenResponse;
import com.factusimple.auth.entity.PasswordResetToken;
import com.factusimple.auth.repository.PasswordResetTokenRepository;
import com.factusimple.auth.repository.TokenRepository;
import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.infrastructure.exception.DomainExceptions.BadRequestException;
import com.factusimple.infrastructure.exception.DomainExceptions.ConflictException;
import com.factusimple.infrastructure.exception.DomainExceptions.ForbiddenException;
import com.factusimple.infrastructure.exception.DomainExceptions.UnauthorizedException;
import com.factusimple.infrastructure.security.JwtService;
import com.factusimple.plan.entity.Plan;
import com.factusimple.plan.repository.PlanRepository;
import com.factusimple.user.entity.Role;
import com.factusimple.user.entity.User;
import com.factusimple.user.repository.UserRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquesta el flujo de autenticación: registro, login, refresh, logout, etc. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_PLAN = "FREE";

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthTokenService authTokenService;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       PlanRepository planRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthTokenService authTokenService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authTokenService = authTokenService;
        this.appProperties = appProperties;
    }

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "El correo ya está registrado");
        }
        Plan freePlan = planRepository.findByCode(DEFAULT_PLAN).orElse(null);

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(Role.USER);
        user.setPlan(freePlan);
        user.setEnabled(false);
        userRepository.save(user);

        String activationToken = jwtService.generateActivationToken(user);
        log.info("Token de activación para {}: {}", user.getEmail(), activationToken);
        return activationToken;
    }

    @RateLimiter(name = "auth-login")
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Credenciales inválidas");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ForbiddenException("ACCOUNT_LOCKED",
                    "Cuenta bloqueada temporalmente por intentos fallidos");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            registerFailedAttempt(user);
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Credenciales inválidas");
        }
        if (!user.isEnabled()) {
            throw new ForbiddenException("ACCOUNT_NOT_ACTIVATED", "La cuenta no está activada");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return authTokenService.issueTokens(user);
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= appProperties.lockout().maxAttempts()) {
            user.setLockedUntil(Instant.now().plus(appProperties.lockout().duration()));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (Exception e) {
            throw new UnauthorizedException("INVALID_TOKEN", "Refresh token inválido o expirado");
        }
        if (!"REFRESH".equals(jwtService.extractType(claims))) {
            throw new UnauthorizedException("INVALID_TOKEN", "El token no es de tipo refresh");
        }
        boolean active = tokenRepository.findByToken(request.refreshToken())
                .map(t -> !t.isRevoked())
                .orElse(false);
        if (!active) {
            throw new UnauthorizedException("INVALID_TOKEN", "Refresh token revocado");
        }
        User user = userRepository.findById(jwtService.extractUserId(claims))
                .orElseThrow(() -> new UnauthorizedException("INVALID_TOKEN", "Usuario inexistente"));
        authTokenService.revokeAll(user);
        return authTokenService.issueTokens(user);
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        userRepository.findById(userId).ifPresent(authTokenService::revokeAll);
    }

    @Transactional
    public void activate(ActivateRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.token());
        } catch (Exception e) {
            throw new BadRequestException("INVALID_TOKEN", "Token de activación inválido o expirado");
        }
        if (!"ACTIVATION".equals(jwtService.extractType(claims))) {
            throw new BadRequestException("INVALID_TOKEN", "El token no es de activación");
        }
        User user = userRepository.findById(jwtService.extractUserId(claims))
                .orElseThrow(() -> new BadRequestException("INVALID_TOKEN", "Usuario inexistente"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    /** Responde de forma uniforme; no revela si el correo existe. */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String rawToken = generateRandomToken();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setTokenHash(sha256(rawToken));
            prt.setExpiresAt(Instant.now().plusSeconds(900)); // 15 min
            prt.setUsed(false);
            resetTokenRepository.save(prt);
            log.info("Token de reset para {}: {}", user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = resetTokenRepository
                .findByTokenHashAndUsedFalse(sha256(request.token()))
                .orElseThrow(() -> new BadRequestException("INVALID_TOKEN", "Token inválido"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("TOKEN_EXPIRED", "El token ha expirado");
        }
        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        prt.setUsed(true);
        resetTokenRepository.save(prt);
        authTokenService.revokeAll(user);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hashear el token", e);
        }
    }
}
