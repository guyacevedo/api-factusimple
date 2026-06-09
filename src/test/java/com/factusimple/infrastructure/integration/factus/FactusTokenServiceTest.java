package com.factusimple.infrastructure.integration.factus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.factusimple.infrastructure.security.EncryptionService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class FactusTokenServiceTest {

    @Mock private RestClient restClient;
    @Mock private FactusProperties properties;
    @Mock private FactusTokenRepository tokenRepository;
    @Mock private EncryptionService encryptionService;

    @Test
    void valid_cached_token_is_reused_without_calling_factus() {
        // Arrange: token aún válido (expira dentro de 1 hora).
        FactusToken stored = new FactusToken();
        stored.setAccessTokenEncrypted("enc-access");
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        when(tokenRepository.findFirstByEstablishmentIsNullOrderByCreatedAtDesc())
                .thenReturn(Optional.of(stored));
        when(encryptionService.decrypt("enc-access")).thenReturn("plain-access");

        FactusTokenService service = new FactusTokenService(
                restClient, properties, tokenRepository, encryptionService);

        // Act
        String token = service.getValidAccessToken();

        // Assert: devuelve el token descifrado y NO contacta a Factus.
        assertThat(token).isEqualTo("plain-access");
        verifyNoInteractions(restClient);
    }
}
