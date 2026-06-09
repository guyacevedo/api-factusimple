package com.factusimple.infrastructure.integration.factus.dto;

/**
 * Resultado normalizado de validar un documento (factura o nota de crédito)
 * en Factus. Se extrae de forma defensiva de la respuesta del proveedor.
 */
public record FactusDocumentResult(
        String number,
        String cufeOrCude,
        boolean validated,
        String validatedAt,
        String qrUrl,
        String errors,
        String rawJson
) {
}
