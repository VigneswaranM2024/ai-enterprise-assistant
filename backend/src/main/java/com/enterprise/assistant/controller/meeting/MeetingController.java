package com.enterprise.assistant.controller.meeting;

import com.enterprise.assistant.dto.response.ApiResponse;
import com.enterprise.assistant.dto.response.MeetingResponse;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.document.extractor.DocumentTextExtractor;
import com.enterprise.assistant.service.meeting.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;
    private final List<DocumentTextExtractor> extractors;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT_UPLOAD')")
    public ResponseEntity<ApiResponse<MeetingResponse>> uploadMeeting(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("meetingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate meetingDate
    ) {
        try {
            String initialMimeType = file.getContentType();
            if (initialMimeType == null) {
                initialMimeType = "application/octet-stream";
            }
            final String mimeType = initialMimeType;

            DocumentTextExtractor suitableExtractor = extractors.stream()
                    .filter(ext -> ext.supports(mimeType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported file type for meeting transcript: " + mimeType));

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            String transcriptText = suitableExtractor.extractText(resource, mimeType);
            
            MeetingResponse response = meetingService.processMeetingTranscript(currentUser, title, meetingDate, transcriptText);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Meeting processed successfully", response));
            
        } catch (Exception e) {
            log.error("Failed to upload meeting transcript", e);
            throw new RuntimeException("Failed to upload meeting transcript: " + e.getMessage(), e);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> getMeetings(
            @AuthenticationPrincipal UserPrincipal currentUser,
            Pageable pageable
    ) {
        Page<MeetingResponse> meetings = meetingService.getTenantMeetings(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success("Meetings retrieved", meetings));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        MeetingResponse meeting = meetingService.getMeetingDetails(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Meeting retrieved", meeting));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id
    ) {
        meetingService.deleteMeeting(currentUser, id);
        return ResponseEntity.ok(ApiResponse.success("Meeting deleted", null));
    }
}
