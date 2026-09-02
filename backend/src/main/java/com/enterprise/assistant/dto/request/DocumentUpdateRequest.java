package com.enterprise.assistant.dto.request;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating document metadata.
 */
public record DocumentUpdateRequest(
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,
    
    String description,
    
    String category,
    
    String securityClassification,
    
    String[] tags
) {}
