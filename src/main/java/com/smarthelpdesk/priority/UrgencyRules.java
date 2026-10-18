package com.smarthelpdesk.priority;

import java.util.List;

public final class UrgencyRules {

    private UrgencyRules() {
    }

    // Category weights
    public static final int WEIGHT_CATEGORY_EXAMINATION = 25;
    public static final int WEIGHT_CATEGORY_IT_SUPPORT = 20;
    public static final int WEIGHT_CATEGORY_FEES_AND_PAYMENTS = 20;
    public static final int WEIGHT_CATEGORY_CAMPUS_FACILITIES = 15;
    public static final int WEIGHT_CATEGORY_TRANSPORT = 10;
    public static final int WEIGHT_CATEGORY_TIMETABLE = 10;
    public static final int WEIGHT_CATEGORY_STUDENT_ADMINISTRATION = 10;
    public static final int WEIGHT_CATEGORY_OTHER = 5;

    // Keyword weights and caps
    public static final int WEIGHT_KEYWORD_STRONG = 10;
    public static final int WEIGHT_KEYWORD_WEAK = 5;
    public static final int KEYWORD_MAX_SCORE = 10;

    // Strong keywords (+10)
    public static final List<String> STRONG_KEYWORDS = List.of(
            "down",
            "outage",
            "not working",
            "cannot login",
            "unable to login",
            "payment failed",
            "hall ticket",
            "unsafe",
            "leak",
            "fire"
    );

    // Weak keywords (+5)
    public static final List<String> WEAK_KEYWORDS = List.of(
            "slow",
            "delay",
            "error",
            "broken",
            "late"
    );

    // Completely ignored keywords (no points)
    public static final List<String> IGNORED_KEYWORDS = List.of(
            "urgent",
            "asap",
            "immediately",
            "emergency",
            "high priority"
    );

    // Student count thresholds and points
    public static final int COUNT_THRESHOLD_TIER_1 = 3;   // 3-4 gives 10
    public static final int COUNT_THRESHOLD_TIER_2 = 5;   // 5-9 gives 25
    public static final int COUNT_THRESHOLD_TIER_3 = 10;  // 10+ gives 40

    public static final int COUNT_POINTS_TIER_0 = 0;   // 1-2
    public static final int COUNT_POINTS_TIER_1 = 10;  // 3-4
    public static final int COUNT_POINTS_TIER_2 = 25;  // 5-9
    public static final int COUNT_POINTS_TIER_3 = 40;  // 10+

    // Incident severity points
    public static final int SEVERITY_POINTS_LOW = 5;
    public static final int SEVERITY_POINTS_MEDIUM = 10;
    public static final int SEVERITY_POINTS_HIGH = 20;
    public static final int SEVERITY_POINTS_CRITICAL = 30;
    public static final int SEVERITY_POINTS_NONE = 0;

    // Level thresholds
    public static final int THRESHOLD_MEDIUM = 20;
    public static final int THRESHOLD_HIGH = 40;
    public static final int THRESHOLD_URGENT = 60;
    public static final int MAX_SCORE = 100;
}
