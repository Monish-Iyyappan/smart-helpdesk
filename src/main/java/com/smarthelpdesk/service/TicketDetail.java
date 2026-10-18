package com.smarthelpdesk.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Record holding full ticket details, computed urgency, and comments for UI and service consumers.
 */
public record TicketDetail(
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
        String createdByName,
        String assignedAgentName,
        String incidentTitle,
        int participantCount,
        List<String> comments
) {
    public String priority() {
        return requestedPriority;
    }
}
