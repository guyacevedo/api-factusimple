package com.factusimple.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.factusimple.infrastructure.config.AppProperties;
import com.factusimple.infrastructure.config.AppProperties.Security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Clave AES-256 válida (Base64 de 32 bytes).
        String key = java.util.Base64.getEncoder().encodeToString(new byte[32]);
        AppProperties props = new AppProperties(new Security(null, key), null, null);
        encryptionService = new EncryptionService(props);
    }

    @Test
    void encrypt_then_decrypt_returns_original() {
        // Arrange
        String plaintext = "token-secreto-de-factus";

        // Act
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Assert
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_same_value_twice_produces_different_ciphertext() {
        // Arrange
        String plaintext = "mismo-valor";

        // Act
        String a = encryptionService.encrypt(plaintext);
        String b = encryptionService.encrypt(plaintext);

        // Assert: el IV aleatorio garantiza textos cifrados distintos.
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void decrypt_tampered_ciphertext_fails() {
        // Arrange
        String encrypted = encryptionService.encrypt("dato");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";

        // Act + Assert
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }
}
