package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SqlGenerationRequest {
    @NotBlank
    private String prompt;
    private String schemaContext = "No schema provided.";
    private String dialect = "PostgreSQL";
}
