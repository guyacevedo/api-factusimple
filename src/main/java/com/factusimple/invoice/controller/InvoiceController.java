package com.factusimple.invoice.controller;

import com.factusimple.infrastructure.exception.ApiResponse;
import com.factusimple.invoice.dto.InvoiceDtos.CreateRequest;
import com.factusimple.invoice.dto.InvoiceDtos.Response;
import com.factusimple.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Facturas", description = "Emisión y consulta de facturas electrónicas vía Factus")
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Operation(summary = "Crear y validar factura",
            description = "Calcula importes en el servidor, consume cupo del plan y valida ante la DIAN vía Factus.")
    @PostMapping
    public ResponseEntity<ApiResponse<Response>> create(
            @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.create(request), "Factura procesada"));
    }

    @Operation(summary = "Listar facturas del establecimiento")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Response>>> list(Pageable pageable) {
        Page<Response> page = invoiceService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(page.getContent(),
                "Página " + page.getNumber() + " de " + page.getTotalPages(),
                null, java.time.Instant.now()));
    }

    @Operation(summary = "Ver una factura")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.get(id)));
    }

    @Operation(summary = "Descargar PDF (Base64)",
            description = "Devuelve el PDF de la factura en Base64 obtenido de Factus.")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<ApiResponse<Map<String, String>>> pdf(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("pdfBase64", invoiceService.downloadPdf(id))));
    }
}
