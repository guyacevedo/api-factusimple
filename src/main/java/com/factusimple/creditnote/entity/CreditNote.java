package com.factusimple.creditnote.entity;

import com.factusimple.establishment.entity.Establishment;
import com.factusimple.infrastructure.persistence.BaseEntity;
import com.factusimple.invoice.entity.DocumentStatus;
import com.factusimple.invoice.entity.Invoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Nota de crédito que anula/corrige una factura validada. */
@Getter
@Setter
@Entity
@Table(name = "credit_notes",
        uniqueConstraints = @UniqueConstraint(name = "uq_credit_note_reference",
                columnNames = {"establishment_id", "reference_code"}))
public class CreditNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    /** Factura que esta nota de crédito anula/corrige. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "reference_code", nullable = false, length = 80)
    private String referenceCode;

    /** Concepto de corrección DIAN. */
    @Column(name = "correction_concept_code", nullable = false, length = 10)
    private String correctionConceptCode;

    @Column(length = 40)
    private String number;

    @Column(length = 255)
    private String cude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "is_validated", nullable = false)
    private boolean validated = false;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String errors;
}
