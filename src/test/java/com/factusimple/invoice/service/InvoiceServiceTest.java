package com.factusimple.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.factusimple.establishment.entity.Establishment;
import com.factusimple.establishment.repository.EstablishmentRepository;
import com.factusimple.infrastructure.integration.factus.FactusClient;
import com.factusimple.infrastructure.integration.factus.dto.FactusDocumentResult;
import com.factusimple.infrastructure.security.CurrentUser;
import com.factusimple.invoice.dto.InvoiceDtos.CreateRequest;
import com.factusimple.invoice.dto.InvoiceDtos.CustomerDto;
import com.factusimple.invoice.dto.InvoiceDtos.ItemDto;
import com.factusimple.invoice.entity.DocumentStatus;
import com.factusimple.invoice.entity.Invoice;
import com.factusimple.invoice.mapper.InvoiceMapper;
import com.factusimple.invoice.repository.InvoiceRepository;
import com.factusimple.plan.service.PlanLimitService;
import com.factusimple.user.entity.User;
import com.factusimple.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private EstablishmentRepository establishmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlanLimitService planLimitService;
    @Mock private FactusClient factusClient;
    @Mock private InvoiceMapper mapper;
    @Mock private CurrentUser currentUser;

    private InvoiceService invoiceService;
    private final UUID establishmentId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository, establishmentRepository,
                userRepository, planLimitService, factusClient, mapper, currentUser);
    }

    @Test
    void create_computes_totals_on_server_and_marks_validated() {
        // Arrange
        Establishment est = new Establishment();
        est.setNumberingRangeId(8);
        when(currentUser.establishmentId()).thenReturn(establishmentId);
        when(currentUser.id()).thenReturn(userId);
        when(establishmentRepository.findById(establishmentId)).thenReturn(Optional.of(est));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(invoiceRepository.existsByEstablishmentIdAndReferenceCode(any(), anyString()))
                .thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(returnsFirstArg());
        when(factusClient.validateBill(any(), anyString())).thenReturn(
                new FactusDocumentResult("SETP990000123", "CUFE-1", true,
                        "2026-06-09", "qr", null, "{}"));

        CreateRequest request = new CreateRequest(null, null, null, null,
                new CustomerDto("3", "22233344", null, "2", null, null, "Juan", null, null, null, null),
                List.of(
                        new ItemDto("P1", "Camiseta", new BigDecimal("2"), new BigDecimal("50000"),
                                null, new BigDecimal("19"), null, "94", null),
                        new ItemDto("P2", "Gorra", new BigDecimal("1"), new BigDecimal("30000"),
                                null, new BigDecimal("19"), null, "94", null)));

        // Act
        invoiceService.create(request);

        // Assert: importes calculados en el servidor y estado VALIDATED.
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, atLeastOnce()).save(captor.capture());
        Invoice saved = captor.getValue();
        assertThat(saved.getSubtotal()).isEqualByComparingTo("130000");
        assertThat(saved.getTaxTotal()).isEqualByComparingTo("24700");
        assertThat(saved.getTotal()).isEqualByComparingTo("154700");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.VALIDATED);
        assertThat(saved.getNumber()).isEqualTo("SETP990000123");
        verify(planLimitService).consumeInvoiceQuota(eq(establishmentId), any());
    }

    @Test
    void create_rejects_duplicate_reference_code() {
        // Arrange
        when(currentUser.establishmentId()).thenReturn(establishmentId);
        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.of(new Establishment()));
        when(invoiceRepository.existsByEstablishmentIdAndReferenceCode(any(), eq("DUP")))
                .thenReturn(true);

        CreateRequest request = new CreateRequest("DUP", null, null, null,
                new CustomerDto("3", "1", null, "2", null, null, "C", null, null, null, null),
                List.of(new ItemDto("P", "P", BigDecimal.ONE, new BigDecimal("1000"),
                        null, null, null, null, null)));

        // Act + Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> invoiceService.create(request))
                .hasFieldOrPropertyWithValue("errorCode", "DUPLICATE_REFERENCE_CODE");
    }
}
