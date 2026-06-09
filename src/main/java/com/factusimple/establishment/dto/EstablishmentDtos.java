package com.factusimple.establishment.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class EstablishmentDtos {

    private EstablishmentDtos() {
    }

    public record CreateRequest(
            @NotBlank String name,
            @NotBlank String identification,
            String dv,
            String address,
            String phone,
            String email,
            String municipalityCode,
            Integer numberingRangeId
    ) {
    }

    public record Response(
            UUID id,
            String name,
            String identification,
            String dv,
            String address,
            String phone,
            String email,
            String municipalityCode,
            Integer numberingRangeId
    ) {
    }
}
