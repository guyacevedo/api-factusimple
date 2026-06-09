package com.factusimple.infrastructure.integration.factus.dto;

import com.factusimple.infrastructure.integration.factus.dto.FactusBillRequest.Customer;
import com.factusimple.infrastructure.integration.factus.dto.FactusBillRequest.Item;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Payload para crear y validar una nota de crédito en Factus
 * ({@code POST /v2/credit-notes/validate}). Anula/corrige una factura validada.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FactusCreditNoteRequest(
        @JsonProperty("reference_code") String referenceCode,
        @JsonProperty("correction_concept_code") String correctionConceptCode,
        @JsonProperty("customization_id") String customizationId,
        /** Número (consecutivo DIAN) de la factura que se anula/corrige. */
        @JsonProperty("bill_id") String billId,
        Customer customer,
        List<Item> items
) {
}
