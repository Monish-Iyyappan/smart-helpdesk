package com.smarthelpdesk.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * List DTO for tickets with system-computed urgency and requested priority.
 */
public record TicketListResponse(
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
        Integer createdBy,
        Integer assignedTo,
        Integer incidentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public String priority() {
        return requestedPriority;
    }
}
