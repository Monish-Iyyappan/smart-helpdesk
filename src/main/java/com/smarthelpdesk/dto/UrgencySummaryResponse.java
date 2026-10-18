package com.smarthelpdesk.dto;

import java.util.Map;

public record UrgencySummaryResponse(
        int totalOverdue,
        Map<String, Integer> countsByLevel
) {
}
