package com.enterprise.assistant.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        String title,
        LocalDate meetingDate,
        String participants,
        String summary,
        String decisions,
        String actionItems,
        String risks,
        String status,
        OffsetDateTime createdAt
) {}
