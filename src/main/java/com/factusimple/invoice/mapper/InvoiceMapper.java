package com.factusimple.invoice.mapper;

import com.factusimple.invoice.dto.InvoiceDtos.Response;
import com.factusimple.invoice.entity.Invoice;
import org.springframework.stereotype.Component;

/** Mapeo Invoice -> DTO (manual; ver nota en notas técnicas sobre MapStruct/JDK 24). */
@Component
public class InvoiceMapper {

    public Response toResponse(Invoice i) {
        if (i == null) {
            return null;
        }
        return new Response(
                i.getId(), i.getReferenceCode(), i.getNumber(), i.getCufe(),
                i.getStatus().name(), i.isValidated(), i.getValidatedAt(),
                i.getSubtotal(), i.getTaxTotal(), i.getTotal(),
                i.getCustomerName(), i.getErrors(), i.getCreatedAt());
    }
}
