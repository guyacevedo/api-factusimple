package com.factusimple.invoice.mapper;

import com.factusimple.invoice.dto.InvoiceDtos.Response;
import com.factusimple.invoice.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "status", expression = "java(invoice.getStatus().name())")
    Response toResponse(Invoice invoice);
}
