package com.factusimple.plan.entity;

import com.factusimple.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Plan de suscripción con sus límites. */
@Getter
@Setter
@Entity
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** Límite de facturas por mes; {@code null} = ilimitado. */
    @Column(name = "monthly_invoice_limit")
    private Integer monthlyInvoiceLimit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
}
