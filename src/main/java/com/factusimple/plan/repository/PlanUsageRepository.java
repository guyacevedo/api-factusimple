package com.factusimple.plan.repository;

import com.factusimple.plan.entity.PlanUsage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanUsageRepository extends JpaRepository<PlanUsage, UUID> {

    /** Garantiza la fila de uso del periodo (idempotente). */
    @Modifying
    @Query(value = """
            INSERT INTO plan_usage (id, version, establishment_id, period, invoice_count,
                                    created_at, updated_at)
            VALUES (gen_random_uuid(), 0, :establishmentId, :period, 0, now(), now())
            ON CONFLICT (establishment_id, period) DO NOTHING
            """, nativeQuery = true)
    void ensureRow(@Param("establishmentId") UUID establishmentId,
                   @Param("period") String period);

    /**
     * Incremento atómico del contador SOLO si está por debajo del límite.
     * Devuelve filas afectadas: 1 = consumido, 0 = límite alcanzado.
     * Si {@code limit} es {@code null} el plan es ilimitado.
     */
    @Modifying
    @Query(value = """
            UPDATE plan_usage
               SET invoice_count = invoice_count + 1, updated_at = now()
             WHERE establishment_id = :establishmentId
               AND period = :period
               AND (:limit IS NULL OR invoice_count < :limit)
            """, nativeQuery = true)
    int tryIncrement(@Param("establishmentId") UUID establishmentId,
                     @Param("period") String period,
                     @Param("limit") Integer limit);
}
