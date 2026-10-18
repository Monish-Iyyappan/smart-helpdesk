package com.smarthelpdesk.priority;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Pure Java engine for calculating ticket urgency based on business rules.
 * Does not depend on Spring or database connectivity.
 */
public class UrgencyCalculator {

    public UrgencyResult calculate(UrgencyInput input, LocalDateTime now) {
        if (input == null) {
            throw new IllegalArgumentException("UrgencyInput cannot be null");
        }
        if (now == null) {
            now = LocalDateTime.now();
        }

        List<String> reasons = new ArrayList<>();

        // 1. Category points
        int categoryPoints = 0;
        String cat = input.categoryName() != null ? input.categoryName().trim() : "";
        String matchedCatName = "Other";

        if (cat.equalsIgnoreCase("Examination")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_EXAMINATION;
            matchedCatName = "Examination";
        } else if (cat.equalsIgnoreCase("IT Support")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_IT_SUPPORT;
            matchedCatName = "IT Support";
        } else if (cat.equalsIgnoreCase("Fees and Payments")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_FEES_AND_PAYMENTS;
            matchedCatName = "Fees and Payments";
        } else if (cat.equalsIgnoreCase("Campus Facilities")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_CAMPUS_FACILITIES;
            matchedCatName = "Campus Facilities";
        } else if (cat.equalsIgnoreCase("Transport")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_TRANSPORT;
            matchedCatName = "Transport";
        } else if (cat.equalsIgnoreCase("Timetable")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_TIMETABLE;
            matchedCatName = "Timetable";
        } else if (cat.equalsIgnoreCase("Student Administration")) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_STUDENT_ADMINISTRATION;
            matchedCatName = "Student Administration";
        } else if (!cat.isBlank()) {
            categoryPoints = UrgencyRules.WEIGHT_CATEGORY_OTHER;
            matchedCatName = cat.equalsIgnoreCase("Other") ? "Other" : cat;
        }

        if (categoryPoints > 0) {
            reasons.add("Category " + matchedCatName + " +" + categoryPoints);
        }

        // 2. Keyword points
        String combinedText = ((input.title() != null ? input.title() : "") + " " +
                (input.description() != null ? input.description() : "")).toLowerCase();

        List<String> matchedKeywords = new ArrayList<>();
        int rawKeywordPoints = 0;

        for (String kw : UrgencyRules.STRONG_KEYWORDS) {
            if (matchesKeyword(combinedText, kw)) {
                matchedKeywords.add(kw);
                rawKeywordPoints += UrgencyRules.WEIGHT_KEYWORD_STRONG;
            }
        }

        for (String kw : UrgencyRules.WEAK_KEYWORDS) {
            if (matchesKeyword(combinedText, kw)) {
                matchedKeywords.add(kw);
                rawKeywordPoints += UrgencyRules.WEIGHT_KEYWORD_WEAK;
            }
        }

        int keywordPoints = Math.min(UrgencyRules.KEYWORD_MAX_SCORE, rawKeywordPoints);
        if (keywordPoints > 0) {
            reasons.add("Keywords matched (" + String.join(", ", matchedKeywords) + ") +" + keywordPoints);
        }

        // 3. Count points: max(studentCount, incidentStudentCount)
        int effectiveCount = Math.max(input.studentCount(), input.incidentStudentCount());
        int countPoints = 0;
        if (effectiveCount >= UrgencyRules.COUNT_THRESHOLD_TIER_3) {
            countPoints = UrgencyRules.COUNT_POINTS_TIER_3;
        } else if (effectiveCount >= UrgencyRules.COUNT_THRESHOLD_TIER_2) {
            countPoints = UrgencyRules.COUNT_POINTS_TIER_2;
        } else if (effectiveCount >= UrgencyRules.COUNT_THRESHOLD_TIER_1) {
            countPoints = UrgencyRules.COUNT_POINTS_TIER_1;
        } else {
            countPoints = UrgencyRules.COUNT_POINTS_TIER_0;
        }

        if (countPoints > 0) {
            reasons.add("Affected students (" + effectiveCount + ") +" + countPoints);
        }

        // 4. Incident severity points
        int severityPoints = 0;
        String sev = input.incidentSeverity() != null ? input.incidentSeverity().trim().toUpperCase() : null;
        if ("CRITICAL".equals(sev)) {
            severityPoints = UrgencyRules.SEVERITY_POINTS_CRITICAL;
        } else if ("HIGH".equals(sev)) {
            severityPoints = UrgencyRules.SEVERITY_POINTS_HIGH;
        } else if ("MEDIUM".equals(sev)) {
            severityPoints = UrgencyRules.SEVERITY_POINTS_MEDIUM;
        } else if ("LOW".equals(sev)) {
            severityPoints = UrgencyRules.SEVERITY_POINTS_LOW;
        }

        if (severityPoints > 0) {
            reasons.add("Incident severity " + sev + " +" + severityPoints);
        }

        // 5. Total score (capped at 100)
        int score = Math.min(UrgencyRules.MAX_SCORE, Math.max(0, categoryPoints + keywordPoints + countPoints + severityPoints));

        // 6. Base level
        UrgencyLevel baseLevel;
        if (score >= UrgencyRules.THRESHOLD_URGENT) {
            baseLevel = UrgencyLevel.URGENT;
        } else if (score >= UrgencyRules.THRESHOLD_HIGH) {
            baseLevel = UrgencyLevel.HIGH;
        } else if (score >= UrgencyRules.THRESHOLD_MEDIUM) {
            baseLevel = UrgencyLevel.MEDIUM;
        } else {
            baseLevel = UrgencyLevel.LOW;
        }

        // 7. Deadlines and overdue calculation
        LocalDateTime createdAt = input.createdAt();
        LocalDateTime dueAt = createdAt != null ? createdAt.plusHours(baseLevel.getDeadlineHours()) : null;
        long ageHours = (createdAt != null) ? Math.max(0, Duration.between(createdAt, now).toHours()) : 0;

        String status = input.status() != null ? input.status().trim().toUpperCase() : "";
        boolean isClosedOrResolved = "RESOLVED".equals(status) || "CLOSED".equals(status);

        boolean overdue = false;
        boolean escalated = false;
        UrgencyLevel level = baseLevel;

        if (!isClosedOrResolved && dueAt != null && now.isAfter(dueAt)) {
            overdue = true;
            escalated = true;
            level = baseLevel.next();
            reasons.add("Overdue: escalated from " + baseLevel + " to " + level);
        }

        return new UrgencyResult(baseLevel, level, score, overdue, escalated, dueAt, ageHours, reasons);
    }

    private boolean matchesKeyword(String text, String keyword) {
        String regex = "\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
}
