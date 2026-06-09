package com.factusimple.infrastructure.security;

import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Generación y validación de JWT (HMAC) para access y refresh tokens. */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final AppProperties.Jwt jwtProps;

    public JwtService(AppProperties properties) {
        this.jwtProps = properties.security().jwt();
        this.key = Keys.hmacShaKeyFor(jwtProps.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user, "ACCESS", jwtProps.accessTokenTtl().toMillis());
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, "REFRESH", jwtProps.refreshTokenTtl().toMillis());
    }

    /** Token de activación de cuenta (stateless, vida 24h). */
    public String generateActivationToken(User user) {
        return buildToken(user, "ACTIVATION", java.time.Duration.ofHours(24).toMillis());
    }

    private String buildToken(User user, String type, long ttlMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key)
                .compact();
    }

    /** Parsea y verifica firma + expiración. Lanza excepción si es inválido. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }
}
