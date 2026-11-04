package com.example.usermanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(description = "Standard error body")
public record ErrorResponse(
        @Schema(description = "When the error occurred (UTC)", example = "2026-11-04T16:00:00Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "HTTP reason phrase", example = "Not Found")
        String error,
        @Schema(description = "Machine-readable error code", example = "USER_NOT_FOUND")
        String code,
        @Schema(description = "Human-readable message", example = "User with id 999 not found")
        String message,
        @Schema(description = "Request path", example = "/api/users/999")
        String path,
        @Schema(description = "Field-level validation errors (field -> message)")
        Map<String, String> fieldErrors) {
}
