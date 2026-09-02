package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailGenerationRequest {
    @NotBlank
    private String context;
    private String tone = "Professional";
    private String targetAudience = "Colleagues";
}
