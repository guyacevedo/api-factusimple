package com.factusimple.infrastructure.integration.factus;

import com.factusimple.infrastructure.exception.DomainExceptions.ProviderUnavailableException;
import com.factusimple.infrastructure.exception.DomainExceptions.UnprocessableEntityException;
import com.factusimple.infrastructure.integration.factus.dto.FactusBillRequest;
import com.factusimple.infrastructure.integration.factus.dto.FactusCreditNoteRequest;
import com.factusimple.infrastructure.integration.factus.dto.FactusDocumentResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de negocio de Factus con resiliencia. Inyecta el Bearer token,
 * refresca ante 401 y reintenta una vez, y ante 409 elimina y recrea el
 * documento (factura pendiente de envío a DIAN).
 */
@Component
public class FactusClient {

    private static final Logger log = LoggerFactory.getLogger(FactusClient.class);

    private final RestClient restClient;
    private final FactusTokenService tokenService;
    private final FactusResponseParser parser;
    private final ObjectMapper objectMapper;

    public FactusClient(@Qualifier("factusRestClient") RestClient restClient,
                        FactusTokenService tokenService,
                        FactusResponseParser parser,
                        ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.tokenService = tokenService;
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    private record HttpResult(int status, String body) {
    }

    // ── Facturas ─────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "factus")
    @RateLimiter(name = "factus")
    public FactusDocumentResult validateBill(FactusBillRequest request, String referenceCode) {
        HttpResult result = post("/v2/bills/validate", request);
        if (result.status() == 409) {
            log.info("Factura {} en conflicto (409); eliminando y recreando", referenceCode);
            deleteBill(referenceCode);
            result = post("/v2/bills/validate", request);
        }
        return handle(result);
    }

    public void deleteBill(String referenceCode) {
        delete("/v2/bills/destroy/reference/" + referenceCode);
    }

    public String downloadBillPdf(String number) {
        HttpResult result = get("/v2/bills/" + number + "/download-pdf");
        return extractPdf(result);
    }

    // ── Notas de crédito ─────────────────────────────────────────────────────

    @CircuitBreaker(name = "factus")
    @RateLimiter(name = "factus")
    public FactusDocumentResult validateCreditNote(FactusCreditNoteRequest request,
                                                   String referenceCode) {
        HttpResult result = post("/v2/credit-notes/validate", request);
        if (result.status() == 409) {
            log.info("Nota crédito {} en conflicto (409); eliminando y recreando", referenceCode);
            delete("/v2/credit-notes/destroy/reference/" + referenceCode);
            result = post("/v2/credit-notes/validate", request);
        }
        return handle(result);
    }

    // ── Internos ─────────────────────────────────────────────────────────────

    private FactusDocumentResult handle(HttpResult result) {
        if (result.status() >= 200 && result.status() < 300) {
            return parser.parse(result.body());
        }
        if (result.status() == 422 || result.status() == 400) {
            throw new UnprocessableEntityException("FACTUS_VALIDATION_ERROR",
                    "Factus rechazó el documento: " + truncate(result.body()));
        }
        throw new ProviderUnavailableException("FACTUS_UNAVAILABLE",
                "Error de Factus (HTTP " + result.status() + ")");
    }

    /** POST con Bearer; ante 401 refresca el token y reintenta una vez. */
    private HttpResult post(String uri, Object body) {
        HttpResult result = doPost(uri, body, tokenService.getValidAccessToken());
        if (result.status() == 401) {
            result = doPost(uri, body, tokenService.forceReauthenticate());
        }
        return result;
    }

    private HttpResult doPost(String uri, Object body, String token) {
        try {
            return restClient.post()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((req, res) -> new HttpResult(res.getStatusCode().value(),
                            new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)), false);
        } catch (Exception e) {
            throw new ProviderUnavailableException("FACTUS_UNAVAILABLE",
                    "No se pudo contactar a Factus");
        }
    }

    private HttpResult get(String uri) {
        try {
            String token = tokenService.getValidAccessToken();
            return restClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .exchange((req, res) -> new HttpResult(res.getStatusCode().value(),
                            new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8)), false);
        } catch (Exception e) {
            throw new ProviderUnavailableException("FACTUS_UNAVAILABLE",
                    "No se pudo contactar a Factus");
        }
    }

    private void delete(String uri) {
        try {
            String token = tokenService.getValidAccessToken();
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(uri)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .exchange((req, res) -> new HttpResult(res.getStatusCode().value(), ""), false);
        } catch (Exception e) {
            log.warn("No se pudo eliminar en Factus {}: {}", uri, e.getMessage());
        }
    }

    private String extractPdf(HttpResult result) {
        try {
            JsonNode root = objectMapper.readTree(result.body());
            JsonNode pdf = root.findValue("pdf_base_64_encoded");
            if (pdf != null && pdf.isTextual()) {
                return pdf.asText();
            }
        } catch (Exception ignored) {
            // cae al error de abajo
        }
        throw new ProviderUnavailableException("FACTUS_UNAVAILABLE",
                "No se pudo obtener el PDF de Factus");
    }

    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
