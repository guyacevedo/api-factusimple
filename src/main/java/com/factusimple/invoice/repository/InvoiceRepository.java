package com.factusimple.invoice.repository;

import com.factusimple.invoice.entity.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByEstablishmentId(UUID establishmentId, Pageable pageable);

    Optional<Invoice> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    boolean existsByEstablishmentIdAndReferenceCode(UUID establishmentId, String referenceCode);
}
