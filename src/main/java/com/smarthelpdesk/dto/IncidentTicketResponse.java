package com.smarthelpdesk.dto;

import java.time.LocalDateTime;

/**
 * DTO for tickets linked to an incident, containing all required fields.
 */
public record IncidentTicketResponse(
        Integer id,
        Integer ticketId,
        String ticketNumber,
        String title,
        String description,
        String status,
        String priority,
        String requestedPriority,
        String category,
        String categoryName,
        String assignedAgent,
        String assignedAgentName,
        Integer assignedTo,
        LocalDateTime createdDate,
        LocalDateTime createdAt,
        boolean overdue,
        String urgencyLevel,
        int urgencyScore,
        Integer incidentId
) {}
