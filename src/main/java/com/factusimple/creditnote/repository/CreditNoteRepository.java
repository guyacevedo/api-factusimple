package com.factusimple.creditnote.repository;

import com.factusimple.creditnote.entity.CreditNote;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditNoteRepository extends JpaRepository<CreditNote, UUID> {

    Page<CreditNote> findByEstablishmentId(UUID establishmentId, Pageable pageable);

    Optional<CreditNote> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    boolean existsByEstablishmentIdAndReferenceCode(UUID establishmentId, String referenceCode);
}
