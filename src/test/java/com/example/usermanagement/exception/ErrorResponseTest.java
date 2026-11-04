package com.example.usermanagement.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void serializesAllFields() throws Exception {
        ErrorResponse response = new ErrorResponse(Instant.parse("2026-11-04T16:00:00Z"), 400, "Bad Request",
                "VALIDATION_FAILED", "Validation failed", "/api/users", Map.of("age", "age must be >= 0"));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

        assertThat(mapper.treeToValue(json.get("timestamp"), Instant.class)).isEqualTo(Instant.parse("2026-11-04T16:00:00Z"));
        assertThat(json.get("status").asInt()).isEqualTo(400);
        assertThat(json.get("error").asText()).isEqualTo("Bad Request");
        assertThat(json.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(json.get("message").asText()).isEqualTo("Validation failed");
        assertThat(json.get("path").asText()).isEqualTo("/api/users");
        assertThat(json.get("fieldErrors").get("age").asText()).isEqualTo("age must be >= 0");
    }

    @Test
    void omitsNullFieldErrors() throws Exception {
        ErrorResponse response = new ErrorResponse(Instant.now(), 404, "Not Found", "USER_NOT_FOUND",
                "User with id 1 not found", "/api/users/1", null);

        assertThat(mapper.readTree(mapper.writeValueAsString(response)).has("fieldErrors")).isFalse();
    }

    @Test
    void omitsEmptyFieldErrors() throws Exception {
        ErrorResponse response = new ErrorResponse(Instant.now(), 404, "Not Found", "USER_NOT_FOUND",
                "User with id 1 not found", "/api/users/1", Map.of());

        assertThat(mapper.readTree(mapper.writeValueAsString(response)).has("fieldErrors")).isFalse();
    }

    @Test
    void recordEqualityIsValueBased() {
        Instant now = Instant.now();
        ErrorResponse a = new ErrorResponse(now, 404, "Not Found", "USER_NOT_FOUND", "m", "/p", null);
        ErrorResponse b = new ErrorResponse(now, 404, "Not Found", "USER_NOT_FOUND", "m", "/p", null);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
