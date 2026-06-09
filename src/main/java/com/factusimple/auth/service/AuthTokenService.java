package com.factusimple.auth.service;

import com.factusimple.auth.dto.AuthDtos.TokenResponse;
import com.factusimple.auth.entity.Token;
import com.factusimple.auth.entity.TokenType;
import com.factusimple.auth.repository.TokenRepository;
import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.infrastructure.security.JwtService;
import com.factusimple.user.entity.User;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Emite, persiste y revoca los tokens JWT del usuario. */
@Service
public class AuthTokenService {

    private final JwtService jwtService;
    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;

    public AuthTokenService(JwtService jwtService,
                            TokenRepository tokenRepository,
                            AppProperties appProperties) {
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.appProperties = appProperties;
    }

    /** Emite un par access+refresh nuevos y los persiste. */
    @Transactional
    public TokenResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        persist(user, access, TokenType.ACCESS,
                Instant.now().plus(appProperties.security().jwt().accessTokenTtl()));
        persist(user, refresh, TokenType.REFRESH,
                Instant.now().plus(appProperties.security().jwt().refreshTokenTtl()));
        return TokenResponse.bearer(access, refresh);
    }

    private void persist(User user, String value, TokenType type, Instant expiresAt) {
        Token token = new Token();
        token.setUser(user);
        token.setToken(value);
        token.setType(type);
        token.setRevoked(false);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);
    }

    /** Revoca todos los tokens vigentes del usuario (logout). */
    @Transactional
    public void revokeAll(User user) {
        tokenRepository.revokeAllByUserAndType(user.getId(), TokenType.ACCESS);
        tokenRepository.revokeAllByUserAndType(user.getId(), TokenType.REFRESH);
    }
}
