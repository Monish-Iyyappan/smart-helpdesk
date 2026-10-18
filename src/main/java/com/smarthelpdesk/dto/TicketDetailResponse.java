package com.smarthelpdesk.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for full ticket details including creator name, assigned agent name or null,
 * incident title or null, participant count, and comments with author names.
 */
public record TicketDetailResponse(
        Integer ticketId,
        String ticketNumber,
        String title,
        String description,
        String categoryName,
        String requestedPriority,
        String urgencyLevel,
        int urgencyScore,
        boolean overdue,
        LocalDateTime dueAt,
        List<String> reasons,
        String status,
        String creatorName,
        String createdByName,
        String assignedAgentName,
        String incidentTitle,
        int participantCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentDetailResponse> comments
) {
    public String priority() {
        return requestedPriority;
    }
}
