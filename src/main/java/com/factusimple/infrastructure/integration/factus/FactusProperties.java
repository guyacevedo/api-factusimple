package com.factusimple.infrastructure.integration.factus;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciales y parámetros del proveedor Factus (desde variables de entorno). */
@ConfigurationProperties(prefix = "factus")
public record FactusProperties(
        String endpoint,
        String user,
        String password,
        String clientId,
        String clientSecret,
        Duration connectTimeout,
        Duration readTimeout
) {

    public FactusProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(30);
        }
    }
}
