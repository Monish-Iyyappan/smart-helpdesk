package com.smarthelpdesk.priority;

public enum UrgencyLevel {
    LOW(168),
    MEDIUM(72),
    HIGH(24),
    URGENT(4);

    private final int deadlineHours;

    UrgencyLevel(int deadlineHours) {
        this.deadlineHours = deadlineHours;
    }

    public int getDeadlineHours() {
        return deadlineHours;
    }

    public int deadlineHours() {
        return deadlineHours;
    }

    public UrgencyLevel next() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH, URGENT -> URGENT;
        };
    }
}
