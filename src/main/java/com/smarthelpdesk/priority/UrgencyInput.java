package com.smarthelpdesk.priority;

import java.time.LocalDateTime;

public record UrgencyInput(
        String categoryName,
        String title,
        String description,
        LocalDateTime createdAt,
        String status,
        int studentCount,
        String incidentSeverity,
        int incidentStudentCount
) {
}
