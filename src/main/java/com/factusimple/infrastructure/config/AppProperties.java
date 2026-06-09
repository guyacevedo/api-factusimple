package com.factusimple.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propiedades tipadas de la plataforma (prefijo {@code app}). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Cors cors,
        Lockout lockout
) {

    public record Security(Jwt jwt, String encryptionKey) {
    }

    public record Jwt(
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Protección de fuerza bruta en el login. */
    public record Lockout(int maxAttempts, Duration duration) {

        public Lockout {
            if (maxAttempts <= 0) {
                maxAttempts = 10;
            }
            if (duration == null) {
                duration = Duration.ofMinutes(15);
            }
        }
    }
}
