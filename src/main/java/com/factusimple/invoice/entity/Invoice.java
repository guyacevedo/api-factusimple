package com.factusimple.invoice.entity;

import com.factusimple.establishment.entity.Establishment;
import com.factusimple.infrastructure.persistence.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Factura electrónica estándar emitida a través de Factus. */
@Getter
@Setter
@Entity
@Table(name = "invoices",
        uniqueConstraints = @UniqueConstraint(name = "uq_invoice_reference",
                columnNames = {"establishment_id", "reference_code"}))
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    /** Código de referencia único por tenant (idempotencia frente a Factus). */
    @Column(name = "reference_code", nullable = false, length = 80)
    private String referenceCode;

    /** Consecutivo DIAN devuelto por Factus tras validar. */
    @Column(length = 40)
    private String number;

    @Column(length = 255)
    private String cufe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "is_validated", nullable = false)
    private boolean validated = false;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "customer_identification", length = 40)
    private String customerIdentification;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 180)
    private String customerEmail;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "qr_url", columnDefinition = "text")
    private String qrUrl;

    @Column(columnDefinition = "text")
    private String errors;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    /** Agrega un ítem manteniendo la relación bidireccional. */
    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        this.items.add(item);
    }
}
