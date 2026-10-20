package com.smarthelpdesk.assignment;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * Pure policy rules for ticket assignment and reassignment.
 * Contains no database or I/O access.
 */
@Component
public class AssignmentPolicy {

    public static final String TARGET_STATUS = "ASSIGNED";
    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "ASSIGNED", "IN_PROGRESS");
    private static final Set<String> REOPEN_REQUIRED_STATUSES = Set.of("RESOLVED", "CLOSED");

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
}
