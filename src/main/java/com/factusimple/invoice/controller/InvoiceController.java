package com.factusimple.invoice.controller;

import com.factusimple.infrastructure.exception.ApiResponse;
import com.factusimple.invoice.dto.InvoiceDtos.CreateRequest;
import com.factusimple.invoice.dto.InvoiceDtos.Response;
import com.factusimple.invoice.service.InvoiceService;
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

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Response>> create(
            @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.create(request), "Factura procesada"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Response>>> list(Pageable pageable) {
        Page<Response> page = invoiceService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(page.getContent(),
                "Página " + page.getNumber() + " de " + page.getTotalPages(),
                null, java.time.Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.get(id)));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<ApiResponse<Map<String, String>>> pdf(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("pdfBase64", invoiceService.downloadPdf(id))));
    }
}
