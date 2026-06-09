package com.factusimple.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InvoiceDtos {

    private InvoiceDtos() {
    }

    public record CreateRequest(
            String referenceCode,
            String observation,
            String paymentForm,
            String paymentMethodCode,
            @Valid @NotNull CustomerDto customer,
            @Valid @NotEmpty List<ItemDto> items
    ) {
    }

    public record CustomerDto(
            @NotBlank String identificationDocumentCode,
            @NotBlank String identification,
            String dv,
            @NotBlank String legalOrganizationCode,
            String tributeCode,
            String company,
            String names,
            String address,
            String email,
            String phone,
            String municipalityCode
    ) {
    }

    public record ItemDto(
            @NotBlank String codeReference,
            @NotBlank String name,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal price,
            BigDecimal discountRate,
            BigDecimal taxRate,
            String taxCode,
            String unitMeasureCode,
            String standardCode
    ) {
    }

    public record Response(
            UUID id,
            String referenceCode,
            String number,
            String cufe,
            String status,
            boolean validated,
            Instant validatedAt,
            BigDecimal subtotal,
            BigDecimal taxTotal,
            BigDecimal total,
            String customerName,
            String errors,
            Instant createdAt
    ) {
    }
}
