package com.smarthelpdesk.assignment;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * Pure policy rules for ticket assignment, reassignment, and comment/closure validations.
 * Contains no database or I/O access.
 */
@Component
public class AssignmentPolicy {

    public static final String TARGET_STATUS = "ASSIGNED";
    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "ASSIGNED", "IN_PROGRESS");
    private static final Set<String> REOPEN_REQUIRED_STATUSES = Set.of("RESOLVED", "CLOSED");

    private static final Set<String> ALLOWED_USER_COMMENT_TYPES = Set.of("GENERAL", "ISSUE");
    private static final Set<String> SYSTEM_ONLY_COMMENT_TYPES = Set.of("REASSIGNMENT", "CLOSURE", "REOPEN");

    /**
     * Determines whether the operation is an initial assignment or a reassignment.
     */
    public AssignmentType determineType(Integer currentAgentId) {
        return currentAgentId == null ? AssignmentType.ASSIGN : AssignmentType.REASSIGN;
    }

    /**
     * Returns the target status after a successful assignment or reassignment.
     */
    public String getTargetStatus() {
        return TARGET_STATUS;
    }

    /**
     * Validates all assignment and reassignment constraints.
     *
     * @param ticketStatus    current status of the ticket
     * @param currentAgentId  current assigned agent ID (null if unassigned)
     * @param newAgentId      target agent ID
     * @param newAssigneeRole role of the target agent
     * @param reason          optional for ASSIGN, required (10-500 trimmed chars) for REASSIGN
     * @return the resolved {@link AssignmentType}
     * @throws AssignmentException on constraint violation (400 for bad request, 409 for state conflict)
     */
    public AssignmentType validate(
            String ticketStatus,
            Integer currentAgentId,
            Integer newAgentId,
            String newAssigneeRole,
            String reason
    ) {
        if (newAgentId == null) {
            throw AssignmentException.badRequest("New assignee ID cannot be null");
        }

        if (newAssigneeRole == null || !"AGENT".equalsIgnoreCase(newAssigneeRole)) {
            throw AssignmentException.badRequest("Target user must have AGENT role");
        }

        if (ticketStatus == null) {
            throw AssignmentException.badRequest("Ticket status cannot be null");
        }

        String normalizedStatus = ticketStatus.trim().toUpperCase();

        if (REOPEN_REQUIRED_STATUSES.contains(normalizedStatus)) {
            throw AssignmentException.conflict(
                    "Tickets in " + normalizedStatus + " status cannot be assigned; admin must reopen first"
            );
        }

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw AssignmentException.conflict(
                    "Tickets in " + normalizedStatus + " status cannot be assigned"
            );
        }

        AssignmentType type = determineType(currentAgentId);

        if (type == AssignmentType.ASSIGN) {
            if (!"OPEN".equalsIgnoreCase(normalizedStatus)) {
                throw AssignmentException.conflict(
                        "Ticket must be in OPEN status for initial assignment. Current status: " + normalizedStatus
                );
            }
        } else {
            // REASSIGN
            if (Objects.equals(currentAgentId, newAgentId)) {
                throw AssignmentException.badRequest("New agent must differ from the current assigned agent");
            }

            if (reason == null || reason.isBlank()) {
                throw AssignmentException.badRequest("Reason is required for ticket reassignment");
            }

            String trimmedReason = reason.trim();
            if (trimmedReason.length() < 10 || trimmedReason.length() > 500) {
                throw AssignmentException.badRequest(
                        "Reassignment reason must be between 10 and 500 characters (got " + trimmedReason.length() + ")"
                );
            }
        }

        return type;
    }

    /**
     * Formats the audit comment for a reassignment.
     */
    public String formatReassignmentComment(String fromAgentName, String toAgentName, String reason) {
        String trimmedReason = reason != null ? reason.trim() : "";
        return "Reassigned from " + fromAgentName + " to " + toAgentName + ". Reason: " + trimmedReason;
    }

    /**
     * Validates a closure note. Note must be non-null and have trimmed length between 10 and 500 characters.
     *
     * @param note raw closure note
     * @return trimmed closure note
     * @throws IllegalArgumentException if null or not between 10 and 500 trimmed characters
     */
    public static String validateClosureNote(String note) {
        if (note == null) {
            throw new IllegalArgumentException("Closure note cannot be null");
        }
        String trimmed = note.trim();
        if (trimmed.length() < 10 || trimmed.length() > 500) {
            throw new IllegalArgumentException(
                    "Closure note must be between 10 and 500 characters (got " + trimmed.length() + ")"
            );
        }
        return trimmed;
    }

    /**
     * Pure validation for reopening a ticket.
     * Ticket must be in RESOLVED status; CLOSED tickets cannot be changed.
     * Reopen reason is required, trimmed length 10-500.
     *
     * @param reason raw reopen reason
     * @param ticketStatus current ticket status
     * @return trimmed reopen reason
     * @throws IllegalArgumentException if reason is null or not 10-500 trimmed chars (maps to 400)
     * @throws IllegalStateException if ticket status is not RESOLVED (maps to 409)
     */
    public static String validateReopen(String reason, String ticketStatus) {
        if (ticketStatus == null) {
            throw new IllegalStateException("Ticket status cannot be null");
        }
        String normalizedStatus = ticketStatus.trim().toUpperCase();
        if ("CLOSED".equals(normalizedStatus)) {
            throw new IllegalStateException("Closed tickets cannot be changed");
        }
        if (!"RESOLVED".equals(normalizedStatus)) {
            throw new IllegalStateException(
                    "Ticket must be in RESOLVED status to be reopened (current status: " + normalizedStatus + ")"
            );
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reopen reason cannot be null");
        }
        String trimmed = reason.trim();
        if (trimmed.length() < 10 || trimmed.length() > 500) {
            throw new IllegalArgumentException(
                    "Reopen reason must be between 10 and 500 characters (got " + trimmed.length() + ")"
            );
        }
        return trimmed;
    }

    /**
     * Validates user-submitted comment type.
     * Defaults to GENERAL when null or blank.
     * Users may send only GENERAL or ISSUE; REASSIGNMENT and CLOSURE are system-only.
     *
     * @param commentType raw comment type
     * @return normalized comment type (GENERAL or ISSUE)
     * @throws IllegalArgumentException if invalid or system-only
     */
    public static String validateUserCommentType(String commentType) {
        if (commentType == null || commentType.trim().isEmpty()) {
            return "GENERAL";
        }
        String upper = commentType.trim().toUpperCase();
        if (SYSTEM_ONLY_COMMENT_TYPES.contains(upper)) {
            throw new IllegalArgumentException(
                    "Comment type " + upper + " is system-only and cannot be sent by users"
            );
        }
        if (!ALLOWED_USER_COMMENT_TYPES.contains(upper)) {
            throw new IllegalArgumentException(
                    "Invalid comment type: " + commentType + ". Allowed types are GENERAL and ISSUE"
            );
        }
        return upper;
    }

    /**
     * Validates that the actor is authorized to submit an ISSUE comment.
     * ISSUE comments are allowed only for STUDENT-role actors who are the ticket creator or a participant.
     *
     * @param actorRole               actor's role
     * @param isCreatorOrParticipant whether actor is creator or participant
     * @throws SecurityException if unauthorized
     */
    public static void validateIssueCommentActor(String actorRole, boolean isCreatorOrParticipant) {
        if (!"STUDENT".equalsIgnoreCase(actorRole) || !isCreatorOrParticipant) {
            throw new SecurityException(
                    "ISSUE comment type is only allowed for student ticket creators or participants"
            );
        }
    }
}
