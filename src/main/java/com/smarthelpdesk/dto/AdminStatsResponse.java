package com.smarthelpdesk.dto;

import java.util.Map;

/**
 * DTO for administrator system statistics.
 */
public record AdminStatsResponse(
        int totalTickets,
        Map<String, Integer> countsByStatus,
        Map<String, Integer> countsByPriority,
        Map<String, Integer> usersByRole,
        int totalIncidents,
        int overdueTickets,
        Map<String, Integer> countsByUrgencyLevel
) {
    public AdminStatsResponse(int totalTickets,
                              Map<String, Integer> countsByStatus,
                              Map<String, Integer> countsByPriority,
                              Map<String, Integer> usersByRole,
                              int totalIncidents) {
        this(totalTickets, countsByStatus, countsByPriority, usersByRole, totalIncidents, 0, Map.of());
    }
}
