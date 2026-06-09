package com.factusimple.establishment.controller;

import com.factusimple.establishment.dto.EstablishmentDtos.CreateRequest;
import com.factusimple.establishment.dto.EstablishmentDtos.Response;
import com.factusimple.establishment.service.EstablishmentService;
import com.factusimple.infrastructure.exception.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/establishments")
public class EstablishmentController {

    private final EstablishmentService establishmentService;

    public EstablishmentController(EstablishmentService establishmentService) {
        this.establishmentService = establishmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Response>> create(
            @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(establishmentService.createForCurrentUser(request),
                        "Establecimiento creado"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Response>> getMine() {
        return ResponseEntity.ok(ApiResponse.ok(establishmentService.getMine()));
    }
}
