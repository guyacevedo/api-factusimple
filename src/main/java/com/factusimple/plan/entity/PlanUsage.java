package com.factusimple.plan.entity;

import com.factusimple.establishment.entity.Establishment;
import com.factusimple.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** Contador de uso del plan por establecimiento y periodo (YYYY-MM). */
@Getter
@Setter
@Entity
@Table(name = "plan_usage",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_usage",
                columnNames = {"establishment_id", "period"}))
public class PlanUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(name = "invoice_count", nullable = false)
    private int invoiceCount = 0;
}
