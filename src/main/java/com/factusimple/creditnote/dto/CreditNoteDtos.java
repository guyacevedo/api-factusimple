package com.factusimple.creditnote.dto;

import com.factusimple.invoice.dto.InvoiceDtos.CustomerDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CreditNoteDtos {

    private CreditNoteDtos() {
    }

    public record CreateRequest(
            @NotNull UUID invoiceId,
            @NotBlank String correctionConceptCode,
            String referenceCode,
            @Valid @NotNull CustomerDto customer
    ) {
    }

    public record Response(
            UUID id,
            UUID invoiceId,
            String referenceCode,
            String correctionConceptCode,
            String number,
            String cude,
            String status,
            boolean validated,
            Instant validatedAt,
            BigDecimal total,
            String errors,
            Instant createdAt
    ) {
    }
}
