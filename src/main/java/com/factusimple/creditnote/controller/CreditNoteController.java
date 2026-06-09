package com.factusimple.creditnote.controller;

import com.factusimple.creditnote.dto.CreditNoteDtos.CreateRequest;
import com.factusimple.creditnote.dto.CreditNoteDtos.Response;
import com.factusimple.creditnote.service.CreditNoteService;
import com.factusimple.infrastructure.exception.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@Tag(name = "Notas de crédito", description = "Anulación/corrección de facturas validadas vía Factus")
@RestController
@RequestMapping("/api/v1/credit-notes")
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    public CreditNoteController(CreditNoteService creditNoteService) {
        this.creditNoteService = creditNoteService;
    }

    @Operation(summary = "Crear y validar nota de crédito",
            description = "Anula/corrige una factura VALIDADA. Valida ante la DIAN vía Factus.")
    @PostMapping
    public ResponseEntity<ApiResponse<Response>> create(
            @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(creditNoteService.create(request), "Nota crédito procesada"));
    }

    @Operation(summary = "Listar notas de crédito del establecimiento")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Response>>> list(Pageable pageable) {
        Page<Response> page = creditNoteService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(page.getContent(),
                "Página " + page.getNumber() + " de " + page.getTotalPages(),
                null, java.time.Instant.now()));
    }

    @Operation(summary = "Ver una nota de crédito")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(creditNoteService.get(id)));
    }
}
