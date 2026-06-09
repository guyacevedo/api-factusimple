package com.factusimple.infrastructure.integration.factus;

import com.factusimple.infrastructure.exception.DomainExceptions.ProviderUnavailableException;
import com.factusimple.infrastructure.integration.factus.dto.FactusTokenResponse;
import com.factusimple.infrastructure.security.EncryptionService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Único punto de obtención, refresco y persistencia (cifrada) del token de
 * Factus. Aplica refresh-or-regenerate y reemplaza el token anterior.
 */
@Service
public class FactusTokenService {

    private static final Logger log = LoggerFactory.getLogger(FactusTokenService.class);
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final FactusProperties properties;
    private final FactusTokenRepository tokenRepository;
    private final EncryptionService encryptionService;

    public FactusTokenService(@Qualifier("factusRestClient") RestClient restClient,
                              FactusProperties properties,
                              FactusTokenRepository tokenRepository,
                              EncryptionService encryptionService) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    /** Devuelve un access token válido, refrescándolo o regenerándolo si hace falta. */
    @Transactional
    public String getValidAccessToken() {
        FactusToken stored = tokenRepository
                .findFirstByEstablishmentIsNullOrderByCreatedAtDesc()
                .orElse(null);

        if (stored == null) {
            return decryptedAccess(authenticate());
        }
        if (stored.getExpiresAt().isAfter(Instant.now().plus(EXPIRY_MARGIN))) {
            return encryptionService.decrypt(stored.getAccessTokenEncrypted());
        }
        // Expirado o por expirar: intentar refresh, si falla regenerar.
        try {
            String refresh = encryptionService.decrypt(stored.getRefreshTokenEncrypted());
            return refresh(refresh, stored);
        } catch (Exception e) {
            log.warn("Refresh de Factus falló, re-autenticando: {}", e.getMessage());
            return decryptedAccess(authenticate());
        }
    }

    /** Fuerza una nueva autenticación (p.ej. tras un 401). */
    @Transactional
    public String forceReauthenticate() {
        return decryptedAccess(authenticate());
    }

    private FactusToken authenticate() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("username", properties.user());
        form.add("password", properties.password());
        FactusTokenResponse response = postToken(form);
        return persist(response);
    }

    private String refresh(String refreshToken, FactusToken previous) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("refresh_token", refreshToken);
        FactusTokenResponse response = postToken(form);
        tokenRepository.delete(previous);
        return decryptedAccess(persist(response));
    }

    private FactusTokenResponse postToken(MultiValueMap<String, String> form) {
        try {
            return restClient.post()
                    .uri("/oauth/token")
                    .header("Accept", "application/json")
                    .body(form)
                    .retrieve()
                    .body(FactusTokenResponse.class);
        } catch (Exception e) {
            throw new ProviderUnavailableException("FACTUS_UNAVAILABLE",
                    "No se pudo autenticar con Factus");
        }
    }

    /** Persiste el token cifrado, reemplazando el anterior global. */
    @Transactional(propagation = Propagation.REQUIRED)
    public FactusToken persist(FactusTokenResponse response) {
        tokenRepository.findFirstByEstablishmentIsNullOrderByCreatedAtDesc()
                .ifPresent(tokenRepository::delete);
        FactusToken token = new FactusToken();
        token.setEstablishment(null);
        token.setAccessTokenEncrypted(encryptionService.encrypt(response.accessToken()));
        token.setRefreshTokenEncrypted(encryptionService.encrypt(response.refreshToken()));
        long ttl = response.expiresIn() != null ? response.expiresIn() : 3600L;
        token.setExpiresAt(Instant.now().plusSeconds(ttl));
        return tokenRepository.save(token);
    }

    private String decryptedAccess(FactusToken token) {
        return encryptionService.decrypt(token.getAccessTokenEncrypted());
    }
}
