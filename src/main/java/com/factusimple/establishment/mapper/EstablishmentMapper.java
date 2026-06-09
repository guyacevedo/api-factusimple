package com.factusimple.establishment.mapper;

import com.factusimple.establishment.dto.EstablishmentDtos.Response;
import com.factusimple.establishment.entity.Establishment;
import org.springframework.stereotype.Component;

/** Mapeo Establishment -> DTO (manual; ver nota en notas técnicas sobre MapStruct/JDK 24). */
@Component
public class EstablishmentMapper {

    public Response toResponse(Establishment e) {
        if (e == null) {
            return null;
        }
        return new Response(
                e.getId(), e.getName(), e.getIdentification(), e.getDv(),
                e.getAddress(), e.getPhone(), e.getEmail(),
                e.getMunicipalityCode(), e.getNumberingRangeId());
    }
}
