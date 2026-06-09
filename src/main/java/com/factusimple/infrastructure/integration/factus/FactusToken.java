package com.factusimple.infrastructure.integration.factus;

import com.factusimple.establishment.entity.Establishment;
import com.factusimple.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Token del proveedor externo Factus. Los valores se almacenan CIFRADOS
 * (AES/GCM); el {@code FactusTokenService} es el único punto de gestión.
 */
@Getter
@Setter
@Entity
@Table(name = "factus_tokens")
public class FactusToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_id")
    private Establishment establishment;

    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "text")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", columnDefinition = "text")
    private String refreshTokenEncrypted;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
