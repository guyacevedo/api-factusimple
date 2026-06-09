package com.factusimple.infrastructure.integration.factus;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FactusTokenRepository extends JpaRepository<FactusToken, UUID> {

    /** Token global de la plataforma (sin establecimiento asociado). */
    Optional<FactusToken> findFirstByEstablishmentIsNullOrderByCreatedAtDesc();
}
