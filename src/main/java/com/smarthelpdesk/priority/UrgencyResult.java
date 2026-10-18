package com.smarthelpdesk.priority;

import java.time.LocalDateTime;
import java.util.List;

public record UrgencyResult(
        UrgencyLevel baseLevel,
        UrgencyLevel level,
        int score,
        boolean overdue,
        boolean escalated,
        LocalDateTime dueAt,
        long ageHours,
        List<String> reasons
) {
}
