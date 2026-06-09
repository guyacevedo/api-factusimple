package com.factusimple.infrastructure.integration.factus;

import com.factusimple.infrastructure.integration.factus.dto.FactusDocumentResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Extrae de forma defensiva los campos relevantes de la respuesta de Factus,
 * cuya estructura exacta puede variar (data.bill.*, data.*, etc.).
 */
@Component
public class FactusResponseParser {

    private final ObjectMapper objectMapper;

    public FactusResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FactusDocumentResult parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String number = firstText(root, "number");
            String cufe = firstText(root, "cufe");
            String cude = firstText(root, "cude");
            String validatedAt = firstText(root, "validated_at");
            boolean validated = firstBoolean(root, "is_validated")
                    || "created".equalsIgnoreCase(firstText(root, "status"));
            String qr = firstText(root, "qr");
            if (qr == null) {
                qr = firstText(root, "qr_image");
            }
            String errors = firstText(root, "errors");
            return new FactusDocumentResult(number,
                    cufe != null ? cufe : cude,
                    validated, validatedAt, qr, errors, json);
        } catch (Exception e) {
            return new FactusDocumentResult(null, null, false, null, null,
                    "No se pudo parsear la respuesta de Factus", json);
        }
    }

    /** Busca recursivamente el primer campo con el nombre dado y lo devuelve como texto. */
    private String firstText(JsonNode node, String field) {
        JsonNode found = find(node, field);
        return found != null && !found.isNull() && found.isValueNode() ? found.asText() : null;
    }

    private boolean firstBoolean(JsonNode node, String field) {
        JsonNode found = find(node, field);
        return found != null && (found.asBoolean(false)
                || "true".equalsIgnoreCase(found.asText("")));
    }

    private JsonNode find(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        if (node.has(field)) {
            return node.get(field);
        }
        for (JsonNode child : node) {
            JsonNode result = find(child, field);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
