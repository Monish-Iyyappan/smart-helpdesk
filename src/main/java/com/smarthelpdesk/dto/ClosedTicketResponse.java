package com.smarthelpdesk.dto;

import java.time.LocalDateTime;

/**
 * DTO for closed ticket log records returned to admins.
 */
public record ClosedTicketResponse(
        String ticketNumber,
        String title,
        String category,
        String assignedAgentName,
        String closedBy,
        LocalDateTime closedAt,
        String closureNote,
        int reassignmentCount
) {}
