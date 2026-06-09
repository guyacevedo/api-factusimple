package com.factusimple.invoice.entity;

/**
 * Estado del documento en su ciclo de vida frente a Factus/DIAN.
 * Nace {@code PENDING} y transiciona según la respuesta del proveedor.
 */
public enum DocumentStatus {
    PENDING,
    VALIDATED,
    REJECTED
}
