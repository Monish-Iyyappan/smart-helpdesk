package com.smarthelpdesk.priority;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrgencyCalculatorTest {

    private UrgencyCalculator calculator;
    private final LocalDateTime fixedCreatedAt = LocalDateTime.of(2026, 10, 18, 10, 0, 0);

    @BeforeEach
    void setUp() {
        calculator = new UrgencyCalculator();
    }

    @Test
    @DisplayName("A lone student with category Examination and strong words stays at MEDIUM or below even with 'urgent' typed in all caps")
    void loneStudentWithExaminationAndStrongWordsStaysAtMediumOrBelowEvenWithUrgentInAllCaps() {
        // Examination = 25 pts.
        // Strong words: "down", "not working" -> +10 each, but keyword total is capped at 10.
        // "URGENT", "ASAP", "EMERGENCY" are in ignored list and must contribute 0.
        // Student count: 1 -> 0 pts. Incident: none -> 0 pts.
        // Total score = 25 + 10 = 35 -> MEDIUM (20-39).
        UrgencyInput input = new UrgencyInput(
                "Examination",
                "URGENT: Hall ticket portal is down",
                "The portal is not working! Need help ASAP and EMERGENCY URGENT HIGH PRIORITY immediately",
                fixedCreatedAt,
                "OPEN",
                1,
                null,
                0
        );

        LocalDateTime now = fixedCreatedAt.plusHours(2);
        UrgencyResult result = calculator.calculate(input, now);

        assertEquals(35, result.score(), "Score should be 35 (25 category + 10 capped keywords)");
        assertEquals(UrgencyLevel.MEDIUM, result.baseLevel(), "Base level should be MEDIUM");
        assertEquals(UrgencyLevel.MEDIUM, result.level(), "Escalated level should stay MEDIUM");
        assertTrue(result.level().ordinal() <= UrgencyLevel.MEDIUM.ordinal(), "Level must stay at MEDIUM or below");
        assertFalse(result.overdue(), "Ticket within 72h deadline should not be overdue");
        assertFalse(result.escalated(), "Ticket should not be escalated");
    }

    @Test
    @DisplayName("5 students plus a CRITICAL incident reaches URGENT")
    void fiveStudentsPlusCriticalIncidentReachesUrgent() {
        // Category Other = 5 pts.
        // 5 students = 25 pts (tier 5-9).
        // CRITICAL incident = 30 pts.
        // Total = 5 + 25 + 30 = 60 pts -> reaches URGENT (60+).
        UrgencyInput input = new UrgencyInput(
                "Other",
                "Campus power line hazard",
                "Live wire exposed near library walkway",
                fixedCreatedAt,
                "OPEN",
                5,
                "CRITICAL",
                5
        );

        LocalDateTime now = fixedCreatedAt.plusHours(1);
        UrgencyResult result = calculator.calculate(input, now);

        assertTrue(result.score() >= 60, "Score must be at least 60");
        assertEquals(UrgencyLevel.URGENT, result.baseLevel(), "Base level must reach URGENT");
        assertEquals(UrgencyLevel.URGENT, result.level(), "Active level must be URGENT");
    }

    @Test
    @DisplayName("The 24-hour boundary for HIGH")
    void twentyFourHourBoundaryForHigh() {
        // IT Support (20) + "cannot login" (10) + 3 students (10) = 40 pts -> HIGH (40-59).
        // HIGH deadline is 24 hours.
        UrgencyInput input = new UrgencyInput(
                "IT Support",
                "Wi-Fi authentication failed",
                "Students cannot login to campus network",
                fixedCreatedAt,
                "OPEN",
                3,
                null,
                0
        );

        assertEquals(UrgencyLevel.HIGH.getDeadlineHours(), 24);

        // Exactly at 24 hours: not after dueAt, so not overdue
        LocalDateTime atBoundary = fixedCreatedAt.plusHours(24);
        UrgencyResult resultAtBoundary = calculator.calculate(input, atBoundary);
        assertEquals(UrgencyLevel.HIGH, resultAtBoundary.baseLevel());
        assertEquals(UrgencyLevel.HIGH, resultAtBoundary.level());
        assertFalse(resultAtBoundary.overdue(), "At exactly 24 hours, ticket is not overdue");
        assertFalse(resultAtBoundary.escalated(), "At exactly 24 hours, ticket is not escalated");

        // 1 second past 24 hours: now is after dueAt, so overdue and escalated to URGENT
        LocalDateTime pastBoundary = fixedCreatedAt.plusHours(24).plusSeconds(1);
        UrgencyResult resultPastBoundary = calculator.calculate(input, pastBoundary);
        assertTrue(resultPastBoundary.overdue(), "Past 24 hours, ticket must be overdue");
        assertTrue(resultPastBoundary.escalated(), "Past 24 hours, ticket must be escalated");
        assertEquals(UrgencyLevel.URGENT, resultPastBoundary.level(), "HIGH ticket must escalate to URGENT");
    }

    @Test
    @DisplayName("Escalation after the deadline")
    void escalationAfterDeadline() {
        // Transport (10) + count 3 (10) = 20 pts -> MEDIUM (deadline 72 hours).
        UrgencyInput mediumTicket = new UrgencyInput(
                "Transport",
                "College bus route issue",
                "Bus schedule delay",
                fixedCreatedAt,
                "IN_PROGRESS",
                3,
                null,
                0
        );

        // At 73 hours: past 72h deadline -> escalates from MEDIUM to HIGH
        LocalDateTime afterMediumDeadline = fixedCreatedAt.plusHours(73);
        UrgencyResult mediumResult = calculator.calculate(mediumTicket, afterMediumDeadline);
        assertEquals(UrgencyLevel.MEDIUM, mediumResult.baseLevel());
        assertTrue(mediumResult.overdue());
        assertTrue(mediumResult.escalated());
        assertEquals(UrgencyLevel.HIGH, mediumResult.level(), "MEDIUM ticket should escalate to HIGH");

        // LOW ticket: Other (5) -> LOW (deadline 168 hours).
        UrgencyInput lowTicket = new UrgencyInput(
                "Other",
                "General query",
                "Inquiry about library books",
                fixedCreatedAt,
                "OPEN",
                1,
                null,
                0
        );

        // At 169 hours: past 168h deadline -> escalates from LOW to MEDIUM
        LocalDateTime afterLowDeadline = fixedCreatedAt.plusHours(169);
        UrgencyResult lowResult = calculator.calculate(lowTicket, afterLowDeadline);
        assertEquals(UrgencyLevel.LOW, lowResult.baseLevel());
        assertTrue(lowResult.overdue());
        assertTrue(lowResult.escalated());
        assertEquals(UrgencyLevel.MEDIUM, lowResult.level(), "LOW ticket should escalate to MEDIUM");

        // URGENT ticket: escalation capped at URGENT
        UrgencyInput urgentTicket = new UrgencyInput(
                "Campus Facilities",
                "Fire emergency in lab",
                "Gas leak and fire alarm ringing",
                fixedCreatedAt,
                "OPEN",
                10,
                "CRITICAL",
                10
        );
        LocalDateTime afterUrgentDeadline = fixedCreatedAt.plusHours(5);
        UrgencyResult urgentResult = calculator.calculate(urgentTicket, afterUrgentDeadline);
        assertEquals(UrgencyLevel.URGENT, urgentResult.baseLevel());
        assertTrue(urgentResult.overdue());
        assertEquals(UrgencyLevel.URGENT, urgentResult.level(), "URGENT ticket cannot escalate beyond URGENT");
    }

    @Test
    @DisplayName("RESOLVED and CLOSED tickets are never overdue")
    void resolvedAndClosedNeverOverdue() {
        // HIGH ticket (deadline 24h)
        UrgencyInput resolvedTicket = new UrgencyInput(
                "IT Support",
                "VPN not working",
                "VPN gateway is down",
                fixedCreatedAt,
                "RESOLVED",
                3,
                null,
                0
        );

        // Evaluated 500 hours later
        LocalDateTime farFuture = fixedCreatedAt.plusHours(500);
        UrgencyResult resolvedResult = calculator.calculate(resolvedTicket, farFuture);

        assertEquals(UrgencyLevel.HIGH, resolvedResult.baseLevel());
        assertFalse(resolvedResult.overdue(), "RESOLVED tickets are never overdue");
        assertFalse(resolvedResult.escalated(), "RESOLVED tickets are never escalated");
        assertEquals(UrgencyLevel.HIGH, resolvedResult.level(), "Level must not escalate for RESOLVED tickets");

        // Also test CLOSED status
        UrgencyInput closedTicket = new UrgencyInput(
                "IT Support",
                "VPN not working",
                "VPN gateway is down",
                fixedCreatedAt,
                "CLOSED",
                3,
                null,
                0
        );

        UrgencyResult closedResult = calculator.calculate(closedTicket, farFuture);
        assertFalse(closedResult.overdue(), "CLOSED tickets are never overdue");
        assertFalse(closedResult.escalated(), "CLOSED tickets are never escalated");
        assertEquals(UrgencyLevel.HIGH, closedResult.level());
    }

    @Test
    @DisplayName("The student requested priority has no effect on urgency calculation")
    void requestedPriorityHasNoEffect() {
        // Two tickets with identical parameters except user attempts to inflate with priority keywords
        UrgencyInput normalTicket = new UrgencyInput(
                "Fees and Payments",
                "Receipt query",
                "Need receipt for semester payment",
                fixedCreatedAt,
                "OPEN",
                1,
                null,
                0
        );

        UrgencyInput inflatedTicket = new UrgencyInput(
                "Fees and Payments",
                "Receipt query",
                "Need receipt for semester payment. URGENT ASAP HIGH PRIORITY EMERGENCY",
                fixedCreatedAt,
                "OPEN",
                1,
                null,
                0
        );

        LocalDateTime now = fixedCreatedAt.plusHours(2);
        UrgencyResult normalResult = calculator.calculate(normalTicket, now);
        UrgencyResult inflatedResult = calculator.calculate(inflatedTicket, now);

        // Requested priority has no effect; ignored words contribute 0 points
        assertEquals(normalResult.score(), inflatedResult.score(), "Ignored words and student priority must not affect score");
        assertEquals(normalResult.baseLevel(), inflatedResult.baseLevel(), "Base levels must be identical");
        assertEquals(normalResult.level(), inflatedResult.level(), "Computed levels must be identical");
        assertEquals(20, normalResult.score(), "Score should be 20 from category Fees and Payments");
        assertEquals(UrgencyLevel.MEDIUM, normalResult.baseLevel());
    }
}
