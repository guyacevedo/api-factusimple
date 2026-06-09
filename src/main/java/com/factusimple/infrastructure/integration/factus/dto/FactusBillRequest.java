package com.factusimple.infrastructure.integration.factus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Payload para crear y validar una factura estándar en Factus
 * ({@code POST /v2/bills/validate}). Nombres de campos según la doc oficial.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FactusBillRequest(
        @JsonProperty("reference_code") String referenceCode,
        @JsonProperty("numbering_range_id") Integer numberingRangeId,
        @JsonProperty("operation_type") String operationType,
        @JsonProperty("send_email") Boolean sendEmail,
        String observation,
        @JsonProperty("payment_details") List<PaymentDetail> paymentDetails,
        Customer customer,
        List<Item> items
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentDetail(
            @JsonProperty("payment_form") String paymentForm,
            @JsonProperty("payment_method_code") String paymentMethodCode,
            String amount,
            @JsonProperty("due_date") String dueDate
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Customer(
            @JsonProperty("identification_document_code") String identificationDocumentCode,
            String identification,
            String dv,
            @JsonProperty("legal_organization_code") String legalOrganizationCode,
            @JsonProperty("tribute_code") String tributeCode,
            String company,
            String names,
            String address,
            String email,
            String phone,
            @JsonProperty("municipality_code") String municipalityCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            @JsonProperty("code_reference") String codeReference,
            String name,
            String quantity,
            String price,
            @JsonProperty("discount_rate") String discountRate,
            @JsonProperty("unit_measure_code") String unitMeasureCode,
            @JsonProperty("standard_code") String standardCode,
            List<Tax> taxes
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tax(
            String code,
            String rate
    ) {
    }
}
