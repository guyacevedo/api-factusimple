package com.factusimple.establishment.mapper;

import com.factusimple.establishment.dto.EstablishmentDtos.Response;
import com.factusimple.establishment.entity.Establishment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstablishmentMapper {

    Response toResponse(Establishment establishment);
}
