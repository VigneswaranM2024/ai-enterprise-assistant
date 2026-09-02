package com.enterprise.assistant.repository.meeting;

import com.enterprise.assistant.domain.meeting.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, UUID> {
    Page<Meeting> findByTenantIdOrderByMeetingDateDesc(UUID tenantId, Pageable pageable);
    Optional<Meeting> findByTenantIdAndId(UUID tenantId, UUID id);
}
