package com.enterprise.assistant.service.meeting;

import com.enterprise.assistant.domain.meeting.Meeting;
import com.enterprise.assistant.domain.meeting.MeetingStatus;
import com.enterprise.assistant.domain.tenant.Tenant;
import com.enterprise.assistant.dto.response.MeetingResponse;
import com.enterprise.assistant.repository.meeting.MeetingRepository;
import com.enterprise.assistant.repository.tenant.TenantRepository;
import com.enterprise.assistant.security.user.UserPrincipal;
import com.enterprise.assistant.service.ai.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final TenantRepository tenantRepository;
    private final LlmService llmService;

    private static final String MEETING_ANALYSIS_PROMPT = """
            You are an expert AI meeting analyst. Your task is to process the following meeting transcript and extract structured information.
            Extract and format the information EXACTLY using the following markdown structure, do NOT add any other text outside this structure:
            
            PARTICIPANTS:
            (list participants here)
            
            SUMMARY:
            (write a concise summary here)
            
            DECISIONS:
            (list key decisions made here)
            
            ACTION ITEMS:
            (list action items here)
            
            RISKS:
            (list potential risks or roadblocks here)
            """;

    @Transactional
    public MeetingResponse processMeetingTranscript(UserPrincipal principal, String title, LocalDate meetingDate, String transcriptText) {
        Tenant tenant = tenantRepository.findById(principal.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        Meeting meeting = Meeting.builder()
                .tenant(tenant)
                .title(title)
                .meetingDate(meetingDate)
                .status(MeetingStatus.PROCESSING)
                .build();
        
        meeting = meetingRepository.save(meeting);

        try {
            log.info("Analyzing meeting transcript for meeting id: {}", meeting.getId());
            String analysisResult = llmService.generateResponse(MEETING_ANALYSIS_PROMPT, transcriptText);
            
            // Parse the structured markdown response
            meeting.setParticipants(extractSection(analysisResult, "PARTICIPANTS:", "SUMMARY:"));
            meeting.setSummary(extractSection(analysisResult, "SUMMARY:", "DECISIONS:"));
            meeting.setDecisions(extractSection(analysisResult, "DECISIONS:", "ACTION ITEMS:"));
            meeting.setActionItems(extractSection(analysisResult, "ACTION ITEMS:", "RISKS:"));
            meeting.setRisks(extractSection(analysisResult, "RISKS:", null));
            meeting.setStatus(MeetingStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Failed to process meeting transcript for meeting id: {}", meeting.getId(), e);
            meeting.setStatus(MeetingStatus.FAILED);
            meeting.setSummary("Failed to analyze transcript: " + e.getMessage());
        }

        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    @Transactional(readOnly = true)
    public Page<MeetingResponse> getTenantMeetings(UserPrincipal principal, Pageable pageable) {
        return meetingRepository.findByTenantIdOrderByMeetingDateDesc(principal.getTenantId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingDetails(UserPrincipal principal, UUID meetingId) {
        Meeting meeting = meetingRepository.findByTenantIdAndId(principal.getTenantId(), meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
        return mapToResponse(meeting);
    }
    
    @Transactional
    public void deleteMeeting(UserPrincipal principal, UUID meetingId) {
        Meeting meeting = meetingRepository.findByTenantIdAndId(principal.getTenantId(), meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Meeting not found"));
        meetingRepository.delete(meeting);
    }

    private String extractSection(String fullText, String startMarker, String endMarker) {
        if (fullText == null) return "";
        int startIndex = fullText.indexOf(startMarker);
        if (startIndex == -1) return "";
        startIndex += startMarker.length();
        
        int endIndex = endMarker != null ? fullText.indexOf(endMarker, startIndex) : fullText.length();
        if (endIndex == -1) endIndex = fullText.length();
        
        return fullText.substring(startIndex, endIndex).trim();
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getMeetingDate(),
                meeting.getParticipants(),
                meeting.getSummary(),
                meeting.getDecisions(),
                meeting.getActionItems(),
                meeting.getRisks(),
                meeting.getStatus().name(),
                meeting.getCreatedAt()
        );
    }
}
