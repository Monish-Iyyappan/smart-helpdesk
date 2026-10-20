package com.smarthelpdesk.assignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentPolicyTest {

    private AssignmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AssignmentPolicy();
    }

    @Test
    @DisplayName("determineType returns ASSIGN when no current agent, REASSIGN when current agent exists")
    void determineTypeReturnsExpectedType() {
        assertEquals(AssignmentType.ASSIGN, policy.determineType(null));
        assertEquals(AssignmentType.REASSIGN, policy.determineType(5));
    }

    @Test
    @DisplayName("Target status after any assign/reassign is always ASSIGNED")
    void targetStatusIsAlwaysAssigned() {
        assertEquals("ASSIGNED", policy.getTargetStatus());
        assertEquals("ASSIGNED", AssignmentPolicy.TARGET_STATUS);
    }

    @Test
    @DisplayName("Initial assignment with OPEN status, AGENT role, and null reason succeeds as ASSIGN")
    void initialAssignmentWithOpenStatusAndAgentRoleSucceedsWithoutReason() {
        AssignmentType type = policy.validate("OPEN", null, 10, "AGENT", null);
        assertEquals(AssignmentType.ASSIGN, type);
    }

    @Test
    @DisplayName("Initial assignment with OPEN status, AGENT role, and optional reason succeeds as ASSIGN")
    void initialAssignmentWithOpenStatusAndAgentRoleSucceedsWithReason() {
        AssignmentType type = policy.validate("OPEN", null, 10, "AGENT", "Initial triage assignment");
        assertEquals(AssignmentType.ASSIGN, type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSIGNED", "IN_PROGRESS", "assigned", "in_progress"})
    @DisplayName("Initial assignment fails with state conflict (409) if ticket status is not OPEN")
    void initialAssignmentFailsWhenStatusIsNotOpen(String nonOpenStatus) {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate(nonOpenStatus, null, 10, "AGENT", null)
        );
        assertTrue(ex.isConflict(), "Must be mapped to 409 conflict when initial assignment is not in OPEN status");
        assertTrue(ex.getMessage().contains("must be in OPEN status"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "CLOSED", "resolved", "closed"})
    @DisplayName("Assignment fails with state conflict (409) if ticket is RESOLVED or CLOSED (reopen required)")
    void assignmentFailsWhenTicketIsResolvedOrClosed(String status) {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate(status, null, 10, "AGENT", null)
        );
        assertTrue(ex.isConflict(), "RESOLVED/CLOSED must result in 409 conflict");
        assertTrue(ex.getMessage().contains("reopen first"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "CLOSED"})
    @DisplayName("Reassignment also fails with state conflict (409) if ticket is RESOLVED or CLOSED")
    void reassignmentFailsWhenTicketIsResolvedOrClosed(String status) {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate(status, 5, 10, "AGENT", "Valid reassignment reason here")
        );
        assertTrue(ex.isConflict(), "RESOLVED/CLOSED must result in 409 conflict");
        assertTrue(ex.getMessage().contains("reopen first"));
    }

    @Test
    @DisplayName("Assignment fails with state conflict (409) on unknown ticket status")
    void assignmentFailsOnUnknownStatus() {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("UNKNOWN_STATUS", null, 10, "AGENT", null)
        );
        assertTrue(ex.isConflict());
    }

    @Test
    @DisplayName("Assignment fails with bad request (400) if ticket status is null")
    void assignmentFailsWhenTicketStatusIsNull() {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate(null, null, 10, "AGENT", null)
        );
        assertFalse(ex.isConflict(), "Null ticket status must be 400 bad request");
        assertTrue(ex.getMessage().contains("Ticket status cannot be null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "ADMIN", "MANAGER", "agent_fake", ""})
    @DisplayName("Assignment fails with bad request (400) when target user role is not AGENT")
    void assignmentFailsWhenTargetUserRoleIsNotAgent(String nonAgentRole) {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("OPEN", null, 10, nonAgentRole, null)
        );
        assertFalse(ex.isConflict(), "Non-AGENT role must be 400 bad request");
        assertTrue(ex.getMessage().contains("AGENT role"));
    }

    @Test
    @DisplayName("Assignment fails with bad request (400) when target user role is null")
    void assignmentFailsWhenTargetUserRoleIsNull() {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("OPEN", null, 10, null, null)
        );
        assertFalse(ex.isConflict());
        assertTrue(ex.getMessage().contains("AGENT role"));
    }

    @Test
    @DisplayName("Assignment fails with bad request (400) when new assignee ID is null")
    void assignmentFailsWhenNewAssigneeIdIsNull() {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("OPEN", null, null, "AGENT", null)
        );
        assertFalse(ex.isConflict(), "Null new agent ID must be 400 bad request");
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "ASSIGNED", "IN_PROGRESS"})
    @DisplayName("Reassignment succeeds with valid reason on OPEN, ASSIGNED, or IN_PROGRESS status")
    void reassignmentSucceedsOnAllowedStatuses(String allowedStatus) {
        AssignmentType type = policy.validate(
                allowedStatus,
                5,
                10,
                "AGENT",
                "Reassigning to tier 2 specialist due to high complexity"
        );
        assertEquals(AssignmentType.REASSIGN, type);
    }

    @Test
    @DisplayName("Reassignment fails with bad request (400) when new agent equals current agent")
    void reassignmentFailsWhenNewAgentMatchesCurrentAgent() {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("ASSIGNED", 5, 5, "AGENT", "Reassigning to the exact same agent")
        );
        assertFalse(ex.isConflict(), "Same agent must be 400 bad request");
        assertTrue(ex.getMessage().contains("differ from the current assigned agent"));
    }

    @Test
    @DisplayName("Reassignment fails with bad request (400) when reason is null or blank")
    void reassignmentFailsWhenReasonIsMissingOrBlank() {
        AssignmentException exNull = assertThrows(AssignmentException.class, () ->
                policy.validate("ASSIGNED", 5, 10, "AGENT", null)
        );
        assertFalse(exNull.isConflict(), "Missing reason must be 400 bad request");
        assertTrue(exNull.getMessage().contains("Reason is required"));

        AssignmentException exBlank = assertThrows(AssignmentException.class, () ->
                policy.validate("ASSIGNED", 5, 10, "AGENT", "    ")
        );
        assertFalse(exBlank.isConflict());
        assertTrue(exBlank.getMessage().contains("Reason is required"));
    }

    @Test
    @DisplayName("Reassignment fails with bad request (400) when trimmed reason length < 10 characters")
    void reassignmentFailsWhenReasonIsTooShort() {
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("ASSIGNED", 5, 10, "AGENT", "  123456789  ") // trimmed length = 9
        );
        assertFalse(ex.isConflict(), "Reason < 10 characters must be 400 bad request");
        assertTrue(ex.getMessage().contains("between 10 and 500"));
    }

    @Test
    @DisplayName("Reassignment fails with bad request (400) when trimmed reason length > 500 characters")
    void reassignmentFailsWhenReasonIsTooLong() {
        String longReason = "a".repeat(501);
        AssignmentException ex = assertThrows(AssignmentException.class, () ->
                policy.validate("ASSIGNED", 5, 10, "AGENT", longReason)
        );
        assertFalse(ex.isConflict(), "Reason > 500 characters must be 400 bad request");
        assertTrue(ex.getMessage().contains("between 10 and 500"));
    }

    @Test
    @DisplayName("Reassignment succeeds when trimmed reason is exactly 10 characters")
    void reassignmentSucceedsAtMinimumLengthBoundary() {
        AssignmentType type = policy.validate("ASSIGNED", 5, 10, "AGENT", "1234567890");
        assertEquals(AssignmentType.REASSIGN, type);
    }

    @Test
    @DisplayName("Reassignment succeeds when trimmed reason is exactly 500 characters")
    void reassignmentSucceedsAtMaximumLengthBoundary() {
        String exact500 = "x".repeat(500);
        AssignmentType type = policy.validate("ASSIGNED", 5, 10, "AGENT", exact500);
        assertEquals(AssignmentType.REASSIGN, type);
    }

    @Test
    @DisplayName("formatReassignmentComment properly formats the audit comment")
    void formatReassignmentCommentFormatsExpectedText() {
        String comment = policy.formatReassignmentComment(
                "Agent Smith",
                "Agent Johnson",
                "Reassigning due to schedule conflict"
        );
        assertEquals(
                "Reassigned from Agent Smith to Agent Johnson. Reason: Reassigning due to schedule conflict",
                comment
        );
    }
}
