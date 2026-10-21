package com.smarthelpdesk.dto;

import com.smarthelpdesk.model.Incident;

import java.time.LocalDateTime;

/**
 * DTO for campus incident details including ticket count and open ticket count.
 */
public record IncidentResponse(
        Integer incidentId,
        String title,
        String description,
        String location,
        String status,
        String severity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int affectedStudents,
        int ticketCount,
        int openTicketCount
) {
    public static IncidentResponse from(Incident incident, int affectedStudents) {
        return from(incident, affectedStudents, 0, 0);
    }

    public static IncidentResponse from(Incident incident, int affectedStudents, int ticketCount, int openTicketCount) {
        if (incident == null) {
            return null;
        }
        return new IncidentResponse(
                incident.getIncidentId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getLocation(),
                incident.getStatus(),
                incident.getSeverity(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                affectedStudents,
                ticketCount,
                openTicketCount
        );
    }
}
