package com.smarthelpdesk.assignment;

import java.time.LocalDateTime;

public record AssignmentRecord(
        Integer historyId,
        Integer ticketId,
        Integer fromAgentId,
        String fromAgentName,
        Integer toAgentId,
        String toAgentName,
        Integer changedById,
        String changedByName,
        AssignmentType assignmentType,
        String reason,
        LocalDateTime changedAt
) {
    public AssignmentRecord(
            Integer historyId,
            Integer ticketId,
            Integer fromAgentId,
            String fromAgentName,
            Integer toAgentId,
            String toAgentName,
            Integer changedById,
            String changedByName,
            AssignmentType assignmentType,
            String reason,
            LocalDateTime changedAt
    ) {
        this.historyId = historyId;
        this.ticketId = ticketId;
        this.fromAgentId = fromAgentId;
        this.fromAgentName = fromAgentName;
        this.toAgentId = toAgentId;
        this.toAgentName = toAgentName;
        this.changedById = changedById;
        this.changedByName = changedByName;
        this.assignmentType = assignmentType != null ? assignmentType : (fromAgentId == null ? AssignmentType.ASSIGN : AssignmentType.REASSIGN);
        this.reason = reason;
        this.changedAt = changedAt;
    }
}
