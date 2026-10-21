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

    @Test
    @DisplayName("validateClosureNote rejects null note with IllegalArgumentException")
    void validateClosureNoteRejectsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateClosureNote(null)
        );
        assertTrue(ex.getMessage().contains("cannot be null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Too short", "  123456789  "})
    @DisplayName("validateClosureNote rejects notes shorter than 10 trimmed characters")
    void validateClosureNoteRejectsTooShort(String shortNote) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateClosureNote(shortNote)
        );
        assertTrue(ex.getMessage().contains("between 10 and 500 characters"));
    }

    @Test
    @DisplayName("validateClosureNote rejects notes longer than 500 trimmed characters")
    void validateClosureNoteRejectsTooLong() {
        String longNote = "x".repeat(501);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateClosureNote(longNote)
        );
        assertTrue(ex.getMessage().contains("between 10 and 500 characters"));
    }

    @Test
    @DisplayName("validateClosureNote trims and accepts valid notes at 10 and 500 boundary lengths")
    void validateClosureNoteAcceptsValidNotesAtBoundaries() {
        assertEquals("1234567890", AssignmentPolicy.validateClosureNote("  1234567890  "));
        String exact500 = "a".repeat(500);
        assertEquals(exact500, AssignmentPolicy.validateClosureNote(" " + exact500 + " "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("validateUserCommentType defaults null or blank to GENERAL")
    void validateUserCommentTypeDefaultsToGeneral(String blank) {
        assertEquals("GENERAL", AssignmentPolicy.validateUserCommentType(null));
        assertEquals("GENERAL", AssignmentPolicy.validateUserCommentType(blank));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GENERAL", "general", "General"})
    @DisplayName("validateUserCommentType accepts GENERAL case-insensitively")
    void validateUserCommentTypeAcceptsGeneral(String general) {
        assertEquals("GENERAL", AssignmentPolicy.validateUserCommentType(general));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISSUE", "issue", "Issue"})
    @DisplayName("validateUserCommentType accepts ISSUE case-insensitively")
    void validateUserCommentTypeAcceptsIssue(String issue) {
        assertEquals("ISSUE", AssignmentPolicy.validateUserCommentType(issue));
    }

    @ParameterizedTest
    @ValueSource(strings = {"REASSIGNMENT", "reassignment", "CLOSURE", "closure", "REOPEN", "reopen"})
    @DisplayName("validateUserCommentType rejects system-only comment types with 400 (IllegalArgumentException)")
    void validateUserCommentTypeRejectsSystemOnlyTypes(String systemType) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateUserCommentType(systemType)
        );
        assertTrue(ex.getMessage().contains("system-only"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"FEEDBACK", "INTERNAL", "STATUS_CHANGE", "RANDOM"})
    @DisplayName("validateUserCommentType rejects unknown comment types with 400 (IllegalArgumentException)")
    void validateUserCommentTypeRejectsUnknownTypes(String unknownType) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateUserCommentType(unknownType)
        );
        assertTrue(ex.getMessage().contains("Invalid comment type"));
    }

    @Test
    @DisplayName("validateIssueCommentActor succeeds for STUDENT creators or participants")
    void validateIssueCommentActorSucceedsForStudentCreatorOrParticipant() {
        AssignmentPolicy.validateIssueCommentActor("STUDENT", true);
        AssignmentPolicy.validateIssueCommentActor("student", true);
    }

    @Test
    @DisplayName("validateIssueCommentActor fails with SecurityException for STUDENT who is not creator/participant")
    void validateIssueCommentActorFailsForUnrelatedStudent() {
        SecurityException ex = assertThrows(SecurityException.class, () ->
                AssignmentPolicy.validateIssueCommentActor("STUDENT", false)
        );
        assertTrue(ex.getMessage().contains("only allowed for student"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"AGENT", "ADMIN", "STAFF", "MANAGER"})
    @DisplayName("validateIssueCommentActor fails with SecurityException for non-STUDENT roles even if participant")
    void validateIssueCommentActorFailsForNonStudentRoles(String nonStudentRole) {
        SecurityException ex = assertThrows(SecurityException.class, () ->
                AssignmentPolicy.validateIssueCommentActor(nonStudentRole, true)
        );
        assertTrue(ex.getMessage().contains("only allowed for student"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "resolved", "Resolved"})
    @DisplayName("validateReopen succeeds on RESOLVED status with valid reason")
    void validateReopenSucceedsOnResolvedStatus(String status) {
        String result = AssignmentPolicy.validateReopen("Valid reopen reason here", status);
        assertEquals("Valid reopen reason here", result);
    }

    @Test
    @DisplayName("validateReopen trims whitespace and accepts valid reasons at 10 and 500 boundaries")
    void validateReopenAcceptsValidReasonsAtBoundaries() {
        assertEquals("1234567890", AssignmentPolicy.validateReopen("  1234567890  ", "RESOLVED"));
        String exact500 = "r".repeat(500);
        assertEquals(exact500, AssignmentPolicy.validateReopen(" " + exact500 + " ", "RESOLVED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLOSED", "closed", "Closed"})
    @DisplayName("validateReopen rejects CLOSED status with 409 (IllegalStateException) stating closed tickets cannot be changed")
    void validateReopenRejectsClosedStatus(String closedStatus) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AssignmentPolicy.validateReopen("Valid reopen reason here", closedStatus)
        );
        assertTrue(ex.getMessage().contains("Closed tickets cannot be changed"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "ASSIGNED", "IN_PROGRESS", "open", "assigned", "in_progress", "PENDING"})
    @DisplayName("validateReopen rejects non-RESOLVED status with 409 (IllegalStateException)")
    void validateReopenRejectsNonResolvedStatus(String nonResolvedStatus) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AssignmentPolicy.validateReopen("Valid reopen reason here", nonResolvedStatus)
        );
        assertTrue(ex.getMessage().contains("Ticket must be in RESOLVED status to be reopened"));
    }

    @Test
    @DisplayName("validateReopen rejects null status with 409 (IllegalStateException)")
    void validateReopenRejectsNullStatus() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                AssignmentPolicy.validateReopen("Valid reopen reason here", null)
        );
        assertTrue(ex.getMessage().contains("Ticket status cannot be null"));
    }

    @Test
    @DisplayName("validateReopen rejects null reason with 400 (IllegalArgumentException)")
    void validateReopenRejectsNullReason() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateReopen(null, "RESOLVED")
        );
        assertTrue(ex.getMessage().contains("Reopen reason cannot be null"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "too short", "123456789", "  123456789  "})
    @DisplayName("validateReopen rejects reason shorter than 10 trimmed characters with 400 (IllegalArgumentException)")
    void validateReopenRejectsTooShortReason(String shortReason) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateReopen(shortReason, "RESOLVED")
        );
        assertTrue(ex.getMessage().contains("between 10 and 500 characters"));
    }

    @Test
    @DisplayName("validateReopen rejects reason longer than 500 trimmed characters with 400 (IllegalArgumentException)")
    void validateReopenRejectsTooLongReason() {
        String tooLong = "a".repeat(501);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                AssignmentPolicy.validateReopen(tooLong, "RESOLVED")
        );
        assertTrue(ex.getMessage().contains("between 10 and 500 characters"));
    }
}
