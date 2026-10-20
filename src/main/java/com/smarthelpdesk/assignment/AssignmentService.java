package com.smarthelpdesk.assignment;

import com.smarthelpdesk.dao.CommentDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.model.Comment;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssignmentService {

    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;
    private final CommentDAO commentDAO;
    private final AssignmentHistoryDao assignmentHistoryDao;
    private final AssignmentPolicy assignmentPolicy;

    public AssignmentService(
            TicketDAO ticketDAO,
            UserDAO userDAO,
            CommentDAO commentDAO,
            AssignmentHistoryDao assignmentHistoryDao,
            AssignmentPolicy assignmentPolicy
    ) {
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
        this.commentDAO = commentDAO;
        this.assignmentHistoryDao = assignmentHistoryDao;
        this.assignmentPolicy = assignmentPolicy;
    }

    private User checkAdmin(Integer actorUserId) {
        if (actorUserId == null) {
            throw new SecurityException("Actor user ID cannot be null");
        }
        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        if (!"ADMIN".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Action requires ADMIN role");
        }
        return actor;
    }

    /**
     * Executes ticket assignment or reassignment with row-level locking.
     *
     * @param ticketId    the ticket ID
     * @param actorUserId the admin performing the action
     * @param newAgentId  the target agent ID
     * @param reason      reassignment reason (required for REASSIGN, optional for ASSIGN)
     * @return the updated ticket
     */
    @Transactional
    public Ticket assignTicket(Integer ticketId, Integer actorUserId, Integer newAgentId, String reason) {
        User admin = checkAdmin(actorUserId);

        if (ticketId == null) {
            throw AssignmentException.badRequest("Ticket ID cannot be null");
        }
        if (newAgentId == null) {
            throw AssignmentException.badRequest("Agent ID cannot be null");
        }

        User targetAgent = userDAO.findById(newAgentId)
                .orElseThrow(() -> AssignmentException.badRequest("Agent not found: " + newAgentId));

        // Lock the ticket row
        Ticket ticket = ticketDAO.findByIdForUpdate(ticketId)
                .orElseThrow(() -> AssignmentException.badRequest("Ticket not found: " + ticketId));

        Integer fromAgentId = ticket.getAssignedTo();

        // Validate pure policy constraints
        AssignmentType type = assignmentPolicy.validate(
                ticket.getStatus(),
                fromAgentId,
                newAgentId,
                targetAgent.getRole(),
                reason
        );

        String fromAgentName = null;
        if (fromAgentId != null) {
            fromAgentName = userDAO.findById(fromAgentId)
                    .map(User::getName)
                    .orElse("Agent " + fromAgentId);
        }

        String trimmedReason = (reason != null && !reason.isBlank()) ? reason.trim() : null;

        // Update ticket in database
        ticketDAO.assign(ticketId, newAgentId);
        ticket.setAssignedTo(newAgentId);
        ticket.setStatus(AssignmentPolicy.TARGET_STATUS);

        // Record history
        assignmentHistoryDao.insert(ticketId, fromAgentId, newAgentId, admin.getUserId(), trimmedReason);

        // For reassignment, add audit comment
        if (type == AssignmentType.REASSIGN) {
            String commentText = assignmentPolicy.formatReassignmentComment(
                    fromAgentName != null ? fromAgentName : "Unknown",
                    targetAgent.getName(),
                    trimmedReason
            );
            Comment comment = new Comment(ticketId, admin.getUserId(), commentText, "REASSIGNMENT");
            commentDAO.insert(comment);
        }

        return ticket;
    }

    /**
     * Returns the assignment history for a ticket (admin only).
     */
    public List<AssignmentRecord> getAssignmentHistory(Integer ticketId, Integer actorUserId) {
        checkAdmin(actorUserId);

        if (ticketId == null) {
            throw AssignmentException.badRequest("Ticket ID cannot be null");
        }

        ticketDAO.findById(ticketId)
                .orElseThrow(() -> AssignmentException.badRequest("Ticket not found: " + ticketId));

        return assignmentHistoryDao.findByTicketId(ticketId);
    }
}
