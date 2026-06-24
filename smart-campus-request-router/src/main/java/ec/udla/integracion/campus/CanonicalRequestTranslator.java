package ec.udla.integracion.campus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class CanonicalRequestTranslator implements Processor {

    private static final String INVALID_REASON = "Mensaje incompleto o con campos obligatorios ausentes";

    private final ObjectMapper objectMapper;

    public CanonicalRequestTranslator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String originalBody = exchange.getIn().getBody(String.class);

        JsonNode originalMessage;
        try {
            originalMessage = objectMapper.readTree(originalBody);
        } catch (Exception ex) {
            ObjectNode invalidMessage = objectMapper.createObjectNode();
            invalidMessage.put("status", "INVALID");
            invalidMessage.put("reason", INVALID_REASON);
            invalidMessage.put("originalMessage", originalBody);

            exchange.setProperty("requestType", "INVALID");
            exchange.getIn().setBody(objectMapper.writeValueAsString(invalidMessage));
            return;
        }

        if (!isValid(originalMessage)) {
            ObjectNode invalidMessage = objectMapper.createObjectNode();
            invalidMessage.put("status", "INVALID");
            invalidMessage.put("reason", INVALID_REASON);
            invalidMessage.set("originalMessage", originalMessage);

            exchange.setProperty("requestType", "INVALID");
            exchange.getIn().setBody(objectMapper.writeValueAsString(invalidMessage));
            return;
        }

        String requestType = originalMessage.get("request_type").asText();

        ObjectNode canonicalMessage = objectMapper.createObjectNode();
        canonicalMessage.put("requestId", originalMessage.get("request_id").asText());

        ObjectNode student = objectMapper.createObjectNode();
        student.put("fullName", originalMessage.get("student_name").asText());
        student.put("document", originalMessage.get("student_document").asText());
        canonicalMessage.set("student", student);

        canonicalMessage.put("type", requestType);
        canonicalMessage.put("sourceChannel", originalMessage.get("channel").asText());
        canonicalMessage.put("createdAt", originalMessage.get("created_at").asText());

        exchange.setProperty("requestType", requestType);
        exchange.getIn().setBody(objectMapper.writeValueAsString(canonicalMessage));
    }

    private boolean isValid(JsonNode message) {
        return hasText(message, "request_id")
                && hasText(message, "student_name")
                && hasText(message, "student_document")
                && hasText(message, "request_type")
                && hasText(message, "channel")
                && hasText(message, "created_at");
    }

    private boolean hasText(JsonNode message, String fieldName) {
        JsonNode field = message.get(fieldName);
        return field != null && field.isTextual() && !field.asText().trim().isEmpty();
    }
}
