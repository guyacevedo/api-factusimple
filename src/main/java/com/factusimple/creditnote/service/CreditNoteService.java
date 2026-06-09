package com.factusimple.creditnote.service;

import com.factusimple.creditnote.dto.CreditNoteDtos.CreateRequest;
import com.factusimple.creditnote.dto.CreditNoteDtos.Response;
import com.factusimple.creditnote.entity.CreditNote;
import com.factusimple.creditnote.repository.CreditNoteRepository;
import com.factusimple.infrastructure.exception.DomainExceptions.ConflictException;
import com.factusimple.infrastructure.exception.DomainExceptions.NotFoundException;
import com.factusimple.infrastructure.exception.DomainExceptions.UnprocessableEntityException;
import com.factusimple.infrastructure.integration.factus.FactusClient;
import com.factusimple.infrastructure.integration.factus.dto.FactusBillRequest;
import com.factusimple.infrastructure.integration.factus.dto.FactusCreditNoteRequest;
import com.factusimple.infrastructure.integration.factus.dto.FactusDocumentResult;
import com.factusimple.infrastructure.security.CurrentUser;
import com.factusimple.invoice.dto.InvoiceDtos.CustomerDto;
import com.factusimple.invoice.entity.DocumentStatus;
import com.factusimple.invoice.entity.Invoice;
import com.factusimple.invoice.repository.InvoiceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final FactusClient factusClient;
    private final CurrentUser currentUser;

    public CreditNoteService(CreditNoteRepository creditNoteRepository,
                             InvoiceRepository invoiceRepository,
                             FactusClient factusClient,
                             CurrentUser currentUser) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.factusClient = factusClient;
        this.currentUser = currentUser;
    }

    @Transactional
    public Response create(CreateRequest request) {
        UUID establishmentId = currentUser.establishmentId();
        Invoice invoice = invoiceRepository
                .findByIdAndEstablishmentId(request.invoiceId(), establishmentId)
                .orElseThrow(() -> new NotFoundException("INVOICE_NOT_FOUND", "Factura no encontrada"));
        if (!invoice.isValidated() || invoice.getStatus() != DocumentStatus.VALIDATED) {
            throw new UnprocessableEntityException("INVOICE_NOT_VALIDATED",
                    "Solo se pueden anular facturas validadas por la DIAN");
        }

        String referenceCode = Optional.ofNullable(request.referenceCode())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> "NC-" + System.currentTimeMillis());
        if (creditNoteRepository.existsByEstablishmentIdAndReferenceCode(establishmentId, referenceCode)) {
            throw new ConflictException("DUPLICATE_REFERENCE_CODE",
                    "Ya existe una nota crédito con ese código de referencia");
        }

        CreditNote note = new CreditNote();
        note.setEstablishment(invoice.getEstablishment());
        note.setInvoice(invoice);
        note.setReferenceCode(referenceCode);
        note.setCorrectionConceptCode(request.correctionConceptCode());
        note.setStatus(DocumentStatus.PENDING);
        note.setTotal(invoice.getTotal());
        note = creditNoteRepository.save(note);

        FactusCreditNoteRequest factusRequest = toFactusRequest(invoice, referenceCode, request);
        FactusDocumentResult result = factusClient.validateCreditNote(factusRequest, referenceCode);
        applyResult(note, result);
        note = creditNoteRepository.save(note);

        return toResponse(note);
    }

    @Transactional(readOnly = true)
    public Page<Response> list(Pageable pageable) {
        return creditNoteRepository.findByEstablishmentId(currentUser.establishmentId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return toResponse(creditNoteRepository
                .findByIdAndEstablishmentId(id, currentUser.establishmentId())
                .orElseThrow(() -> new NotFoundException("CREDIT_NOTE_NOT_FOUND",
                        "Nota crédito no encontrada")));
    }

    private FactusCreditNoteRequest toFactusRequest(Invoice invoice, String referenceCode,
                                                    CreateRequest request) {
        CustomerDto c = request.customer();
        FactusBillRequest.Customer customer = new FactusBillRequest.Customer(
                c.identificationDocumentCode(), c.identification(), c.dv(),
                c.legalOrganizationCode(), c.tributeCode() != null ? c.tributeCode() : "ZZ",
                c.company(), c.names(), c.address(), c.email(), c.phone(), c.municipalityCode());

        List<FactusBillRequest.Item> items = invoice.getItems().stream()
                .map(i -> new FactusBillRequest.Item(
                        i.getCodeReference(), i.getName(),
                        i.getQuantity().toPlainString(), i.getPrice().toPlainString(),
                        i.getDiscountRate().toPlainString(),
                        i.getUnitMeasureCode(), i.getStandardCode(),
                        List.of(new FactusBillRequest.Tax("01", i.getTaxRate().toPlainString()))))
                .toList();

        return new FactusCreditNoteRequest(referenceCode, request.correctionConceptCode(),
                "20", invoice.getNumber(), customer, items);
    }

    private void applyResult(CreditNote note, FactusDocumentResult result) {
        note.setNumber(result.number());
        note.setCude(result.cufeOrCude());
        note.setErrors(result.errors());
        if (result.validated()) {
            note.setValidated(true);
            note.setValidatedAt(Instant.now());
            note.setStatus(DocumentStatus.VALIDATED);
        } else {
            note.setStatus(DocumentStatus.REJECTED);
        }
    }

    private Response toResponse(CreditNote n) {
        return new Response(n.getId(), n.getInvoice().getId(), n.getReferenceCode(),
                n.getCorrectionConceptCode(), n.getNumber(), n.getCude(), n.getStatus().name(),
                n.isValidated(), n.getValidatedAt(), n.getTotal(), n.getErrors(), n.getCreatedAt());
    }
}
