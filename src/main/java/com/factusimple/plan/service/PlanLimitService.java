package com.factusimple.plan.service;

import com.factusimple.infrastructure.exception.DomainExceptions.UnprocessableEntityException;
import com.factusimple.plan.entity.Plan;
import com.factusimple.plan.repository.PlanUsageRepository;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifica y consume de forma atómica el límite de facturas del plan. */
@Service
public class PlanLimitService {

    private final PlanUsageRepository planUsageRepository;

    public PlanLimitService(PlanUsageRepository planUsageRepository) {
        this.planUsageRepository = planUsageRepository;
    }

    /**
     * Consume una unidad del cupo del periodo actual de forma atómica.
     * Lanza {@code PLAN_LIMIT_EXCEEDED} si se alcanzó el límite.
     */
    @Transactional
    public void consumeInvoiceQuota(UUID establishmentId, Plan plan) {
        String period = YearMonth.now().toString();
        Integer limit = plan != null ? plan.getMonthlyInvoiceLimit() : null;
        planUsageRepository.ensureRow(establishmentId, period);
        int updated = planUsageRepository.tryIncrement(establishmentId, period, limit);
        if (updated == 0) {
            throw new UnprocessableEntityException("PLAN_LIMIT_EXCEEDED",
                    "Se alcanzó el límite de facturas del plan para este mes");
        }
    }
}
