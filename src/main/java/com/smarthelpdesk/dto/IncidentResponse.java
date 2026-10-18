package com.smarthelpdesk.dto;

import com.smarthelpdesk.model.Incident;

import java.time.LocalDateTime;

/**
 * DTO for campus incident details including the affected students count.
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
        int affectedStudents
) {
    public static IncidentResponse from(Incident incident, int affectedStudents) {
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
                affectedStudents
        );
    }
}
