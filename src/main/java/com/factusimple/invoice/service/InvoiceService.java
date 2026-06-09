package com.factusimple.invoice.service;

import com.factusimple.establishment.entity.Establishment;
import com.factusimple.establishment.repository.EstablishmentRepository;
import com.factusimple.infrastructure.exception.DomainExceptions.ConflictException;
import com.factusimple.infrastructure.exception.DomainExceptions.NotFoundException;
import com.factusimple.infrastructure.integration.factus.FactusClient;
import com.factusimple.infrastructure.integration.factus.dto.FactusBillRequest;
import com.factusimple.infrastructure.integration.factus.dto.FactusDocumentResult;
import com.factusimple.invoice.dto.InvoiceDtos.CreateRequest;
import com.factusimple.invoice.dto.InvoiceDtos.CustomerDto;
import com.factusimple.invoice.dto.InvoiceDtos.ItemDto;
import com.factusimple.invoice.dto.InvoiceDtos.Response;
import com.factusimple.invoice.entity.DocumentStatus;
import com.factusimple.invoice.entity.Invoice;
import com.factusimple.invoice.entity.InvoiceItem;
import com.factusimple.invoice.mapper.InvoiceMapper;
import com.factusimple.invoice.repository.InvoiceRepository;
import com.factusimple.plan.service.PlanLimitService;
import com.factusimple.user.repository.UserRepository;
import com.factusimple.infrastructure.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final InvoiceRepository invoiceRepository;
    private final EstablishmentRepository establishmentRepository;
    private final UserRepository userRepository;
    private final PlanLimitService planLimitService;
    private final FactusClient factusClient;
    private final InvoiceMapper mapper;
    private final CurrentUser currentUser;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          EstablishmentRepository establishmentRepository,
                          UserRepository userRepository,
                          PlanLimitService planLimitService,
                          FactusClient factusClient,
                          InvoiceMapper mapper,
                          CurrentUser currentUser) {
        this.invoiceRepository = invoiceRepository;
        this.establishmentRepository = establishmentRepository;
        this.userRepository = userRepository;
        this.planLimitService = planLimitService;
        this.factusClient = factusClient;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @Transactional
    public Response create(CreateRequest request) {
        UUID establishmentId = currentUser.establishmentId();
        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new NotFoundException("ESTABLISHMENT_NOT_FOUND",
                        "Establecimiento no encontrado"));

        String referenceCode = Optional.ofNullable(request.referenceCode())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> "INV-" + System.currentTimeMillis());
        if (invoiceRepository.existsByEstablishmentIdAndReferenceCode(establishmentId, referenceCode)) {
            throw new ConflictException("DUPLICATE_REFERENCE_CODE",
                    "Ya existe una factura con ese código de referencia");
        }

        // Límite de plan: verificación y consumo atómico en BD.
        var plan = userRepository.findById(currentUser.id())
                .map(u -> u.getPlan()).orElse(null);
        planLimitService.consumeInvoiceQuota(establishmentId, plan);

        // Construcción + cálculo de importes en el servidor.
        Invoice invoice = buildInvoice(establishment, referenceCode, request);
        invoice = invoiceRepository.save(invoice);

        // Emisión en Factus y actualización del estado.
        FactusBillRequest factusRequest = toFactusRequest(establishment, referenceCode, request, invoice);
        FactusDocumentResult result = factusClient.validateBill(factusRequest, referenceCode);
        applyResult(invoice, result);
        invoice = invoiceRepository.save(invoice);

        return mapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<Response> list(Pageable pageable) {
        return invoiceRepository.findByEstablishmentId(currentUser.establishmentId(), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Response get(UUID id) {
        return mapper.toResponse(loadOwned(id));
    }

    @Transactional(readOnly = true)
    public String downloadPdf(UUID id) {
        Invoice invoice = loadOwned(id);
        if (invoice.getNumber() == null) {
            throw new ConflictException("INVOICE_NOT_VALIDATED",
                    "La factura no tiene número DIAN (no validada)");
        }
        return factusClient.downloadBillPdf(invoice.getNumber());
    }

    private Invoice loadOwned(UUID id) {
        return invoiceRepository.findByIdAndEstablishmentId(id, currentUser.establishmentId())
                .orElseThrow(() -> new NotFoundException("INVOICE_NOT_FOUND", "Factura no encontrada"));
    }

    // ── Construcción / cálculo ────────────────────────────────────────────────

    private Invoice buildInvoice(Establishment establishment, String referenceCode,
                                 CreateRequest request) {
        Invoice invoice = new Invoice();
        invoice.setEstablishment(establishment);
        invoice.setReferenceCode(referenceCode);
        invoice.setStatus(DocumentStatus.PENDING);
        CustomerDto c = request.customer();
        invoice.setCustomerIdentification(c.identification());
        invoice.setCustomerName(c.company() != null ? c.company() : c.names());
        invoice.setCustomerEmail(c.email());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        for (ItemDto item : request.items()) {
            BigDecimal discountRate = nullToZero(item.discountRate());
            BigDecimal taxRate = nullToZero(item.taxRate());
            BigDecimal lineNet = item.price().multiply(item.quantity());
            BigDecimal discount = lineNet.multiply(discountRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal base = lineNet.subtract(discount);
            BigDecimal tax = base.multiply(taxRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(base);
            taxTotal = taxTotal.add(tax);

            InvoiceItem entity = new InvoiceItem();
            entity.setCodeReference(item.codeReference());
            entity.setName(item.name());
            entity.setQuantity(item.quantity());
            entity.setPrice(item.price());
            entity.setDiscountRate(discountRate);
            entity.setTaxRate(taxRate);
            entity.setUnitMeasureCode(item.unitMeasureCode());
            entity.setStandardCode(item.standardCode());
            invoice.addItem(entity);
        }
        invoice.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxTotal(taxTotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotal(subtotal.add(taxTotal).setScale(2, RoundingMode.HALF_UP));
        return invoice;
    }

    private FactusBillRequest toFactusRequest(Establishment establishment, String referenceCode,
                                              CreateRequest request, Invoice invoice) {
        CustomerDto c = request.customer();
        FactusBillRequest.Customer customer = new FactusBillRequest.Customer(
                c.identificationDocumentCode(), c.identification(), c.dv(),
                c.legalOrganizationCode(), c.tributeCode() != null ? c.tributeCode() : "ZZ",
                c.company(), c.names(), c.address(), c.email(), c.phone(), c.municipalityCode());

        List<FactusBillRequest.Item> items = request.items().stream()
                .map(i -> new FactusBillRequest.Item(
                        i.codeReference(), i.name(),
                        i.quantity().toPlainString(), i.price().toPlainString(),
                        nullToZero(i.discountRate()).toPlainString(),
                        i.unitMeasureCode(), i.standardCode(),
                        List.of(new FactusBillRequest.Tax(
                                i.taxCode() != null ? i.taxCode() : "01",
                                nullToZero(i.taxRate()).toPlainString()))))
                .toList();

        FactusBillRequest.PaymentDetail payment = new FactusBillRequest.PaymentDetail(
                request.paymentForm() != null ? request.paymentForm() : "1",
                request.paymentMethodCode() != null ? request.paymentMethodCode() : "10",
                invoice.getTotal().toPlainString(), null);

        return new FactusBillRequest(referenceCode, establishment.getNumberingRangeId(),
                "10", false, request.observation(), List.of(payment), customer, items);
    }

    private void applyResult(Invoice invoice, FactusDocumentResult result) {
        invoice.setNumber(result.number());
        invoice.setCufe(result.cufeOrCude());
        invoice.setQrUrl(result.qrUrl());
        invoice.setErrors(result.errors());
        if (result.validated()) {
            invoice.setValidated(true);
            invoice.setValidatedAt(Instant.now());
            invoice.setStatus(DocumentStatus.VALIDATED);
        } else {
            invoice.setStatus(DocumentStatus.REJECTED);
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
