package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CodeGenerationRequest {
    @NotBlank
    private String prompt;
    private String language = "Java";
}
