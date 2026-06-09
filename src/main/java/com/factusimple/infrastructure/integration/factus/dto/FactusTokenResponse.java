package com.factusimple.infrastructure.integration.factus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta del endpoint OAuth de Factus ({@code /oauth/token}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactusTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("token_type") String tokenType
) {
}
