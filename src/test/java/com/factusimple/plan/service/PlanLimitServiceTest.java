package com.factusimple.plan.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.factusimple.infrastructure.exception.AppException;
import com.factusimple.plan.entity.Plan;
import com.factusimple.plan.repository.PlanUsageRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock
    private PlanUsageRepository planUsageRepository;

    @InjectMocks
    private PlanLimitService planLimitService;

    @Test
    void consume_succeeds_when_under_limit() {
        // Arrange
        UUID est = UUID.randomUUID();
        Plan plan = new Plan();
        plan.setMonthlyInvoiceLimit(50);
        when(planUsageRepository.tryIncrement(eq(est), any(), eq(50))).thenReturn(1);

        // Act + Assert
        assertThatCode(() -> planLimitService.consumeInvoiceQuota(est, plan))
                .doesNotThrowAnyException();
        verify(planUsageRepository).ensureRow(eq(est), any());
    }

    @Test
    void consume_throws_when_limit_reached() {
        // Arrange
        UUID est = UUID.randomUUID();
        Plan plan = new Plan();
        plan.setMonthlyInvoiceLimit(1);
        when(planUsageRepository.tryIncrement(eq(est), any(), eq(1))).thenReturn(0);

        // Act + Assert
        assertThatThrownBy(() -> planLimitService.consumeInvoiceQuota(est, plan))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PLAN_LIMIT_EXCEEDED");
    }
}
