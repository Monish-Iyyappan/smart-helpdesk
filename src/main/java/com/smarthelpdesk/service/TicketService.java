package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.CategoryDAO;
import com.smarthelpdesk.dao.CommentDAO;
import com.smarthelpdesk.dao.IncidentDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.TicketParticipantDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.dto.CommentDetailResponse;
import com.smarthelpdesk.dto.TicketDetailResponse;
import com.smarthelpdesk.model.Category;
import com.smarthelpdesk.model.Comment;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.priority.UrgencyResult;
import com.smarthelpdesk.priority.UrgencyService;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;
    private final CategoryDAO categoryDAO;
    private final CommentDAO commentDAO;
    private final TicketParticipantDAO ticketParticipantDAO;
    private final IncidentDAO incidentDAO;
    private final UrgencyService urgencyService;

    public TicketService(TicketDAO ticketDAO, UserDAO userDAO, CategoryDAO categoryDAO,
                         CommentDAO commentDAO, TicketParticipantDAO ticketParticipantDAO,
                         IncidentDAO incidentDAO, UrgencyService urgencyService) {
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
        this.categoryDAO = categoryDAO;
        this.commentDAO = commentDAO;
        this.ticketParticipantDAO = ticketParticipantDAO;
        this.incidentDAO = incidentDAO;
        this.urgencyService = urgencyService;
    }

    @Transactional
    public Ticket create(Integer actorUserId, Integer categoryId, String title, String description, String priority) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }
        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        if (!"STUDENT".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Only users with role STUDENT can create tickets");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (categoryId == null || categoryDAO.findById(categoryId).isEmpty()) {
            throw new IllegalArgumentException("Category does not exist: " + categoryId);
        }

        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }
        String normPriority = priority.trim().toUpperCase();
        if (!normPriority.equals("LOW") && !normPriority.equals("MEDIUM") &&
            !normPriority.equals("HIGH") && !normPriority.equals("URGENT")) {
            throw new IllegalArgumentException("Invalid priority. Must be LOW, MEDIUM, HIGH, or URGENT");
        }

        Ticket ticket = new Ticket();
        ticket.setCreatedBy(actorUserId);
        ticket.setCategoryId(categoryId);
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setPriority(normPriority);
        ticket.setStatus("OPEN");

        Integer ticketId = ticketDAO.insert(ticket);
        String ticketNumber = "HD-" + (1000 + ticketId);
        ticketDAO.updateTicketNumber(ticketId, ticketNumber);

        ticket.setTicketId(ticketId);
        ticket.setTicketNumber(ticketNumber);

        ticketParticipantDAO.add(ticketId, actorUserId);
        return ticket;
    }

    public boolean join(String ticketNumber, Integer actorUserId) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }
        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        if (!"STUDENT".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Only students can join tickets");
        }
        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket number cannot be blank");
        }

        Ticket ticket = ticketDAO.findByTicketNumber(ticketNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketNumber));

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("Cannot join a CLOSED ticket");
        }

        return ticketParticipantDAO.add(ticket.getTicketId(), actorUserId);
    }

    public Comment addComment(Integer ticketId, Integer actorUserId, String text) {
        if (ticketId == null) {
            throw new IllegalArgumentException("Ticket ID cannot be null");
        }
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be blank");
        }

        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("Cannot add comment to a CLOSED ticket");
        }

        boolean isCreator = actorUserId.equals(ticket.getCreatedBy());
        boolean isParticipant = ticketParticipantDAO.isParticipant(ticketId, actorUserId);
        boolean isAssignedAgent = "AGENT".equalsIgnoreCase(actor.getRole()) && actorUserId.equals(ticket.getAssignedTo());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(actor.getRole());

        if (!isCreator && !isParticipant && !isAssignedAgent && !isAdmin) {
            throw new SecurityException("User is not authorized to comment on this ticket");
        }

        Comment comment = new Comment(ticketId, actorUserId, text.trim());
        ticketDAO.findById(ticketId);
        commentDAO.insert(comment);
        return comment;
    }

    public Ticket updateStatus(Integer ticketId, Integer actorUserId, String newStatus) {
        if (ticketId == null || newStatus == null) {
            throw new IllegalArgumentException("Ticket ID and new status must not be null");
        }
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }

        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        if (!"AGENT".equalsIgnoreCase(actor.getRole())) {
            throw new SecurityException("Only agents can update ticket status");
        }

        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().equals(actorUserId)) {
            throw new SecurityException("Only the assigned agent can update this ticket");
        }

        String currentStatus = ticket.getStatus();
        String targetStatus = newStatus.trim().toUpperCase();

        boolean validTransition = ("ASSIGNED".equalsIgnoreCase(currentStatus) && "IN_PROGRESS".equals(targetStatus)) ||
                                  ("IN_PROGRESS".equalsIgnoreCase(currentStatus) && "RESOLVED".equals(targetStatus));

        if (!validTransition) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        ticketDAO.updateStatus(ticketId, targetStatus);
        ticket.setStatus(targetStatus);
        return ticket;
    }

    public List<Ticket> myTickets(Integer actorUserId) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }
        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));

        if ("STUDENT".equalsIgnoreCase(actor.getRole())) {
            return ticketDAO.findByParticipant(actorUserId);
        } else if ("AGENT".equalsIgnoreCase(actor.getRole())) {
            return ticketDAO.findByAssignedTo(actorUserId);
        } else if ("ADMIN".equalsIgnoreCase(actor.getRole())) {
            return ticketDAO.findAll();
        } else {
            throw new SecurityException("Unknown user role: " + actor.getRole());
        }
    }

    public List<TicketListResponse> myTicketResponses(Integer actorUserId) {
        List<Ticket> tickets = myTickets(actorUserId);
        return urgencyService.evaluateTicketsBatch(tickets);
    }

    public List<Category> listCategories() {
        return categoryDAO.findAll();
    }

    public List<Comment> listComments(Integer ticketId, Integer actorUserId) {
        return getComments(ticketId, actorUserId);
    }

    public TicketDetail getDetail(String ticketNumber, Integer actorUserId) {
        Ticket ticket = getByTicketNumber(ticketNumber, actorUserId);

        String categoryName = categoryDAO.findById(ticket.getCategoryId())
                .map(Category::getCategoryName)
                .orElse("Unknown");

        String creatorName = userDAO.findById(ticket.getCreatedBy())
                .map(User::getName)
                .orElse("Unknown");

        String agentName = "Unassigned";
        if (ticket.getAssignedTo() != null) {
            agentName = userDAO.findById(ticket.getAssignedTo())
                    .map(User::getName)
                    .orElse("Unassigned");
        }

        String incidentTitle = null;
        if (ticket.getIncidentId() != null) {
            incidentTitle = incidentDAO.findById(ticket.getIncidentId())
                    .map(Incident::getTitle)
                    .orElse(null);
        }

        int participantCount = ticketParticipantDAO.findByTicketId(ticket.getTicketId()).size();

        List<Comment> rawComments = commentDAO.findByTicketId(ticket.getTicketId());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<String> formattedComments = new ArrayList<>();
        for (Comment c : rawComments) {
            String author = userDAO.findById(c.getUserId())
                    .map(User::getName)
                    .orElse("User " + c.getUserId());
            String dateStr = c.getCreatedAt() != null ? c.getCreatedAt().format(dtf) : "Unknown date";
            formattedComments.add(String.format("[%s] %s: %s", dateStr, author, c.getCommentText()));
        }

        UrgencyResult urgency = urgencyService.evaluate(ticketNumber, actorUserId);

        return new TicketDetail(
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                categoryName,
                ticket.getPriority(),
                urgency.level().name(),
                urgency.score(),
                urgency.overdue(),
                urgency.dueAt(),
                urgency.reasons(),
                ticket.getStatus(),
                creatorName,
                agentName,
                incidentTitle,
                participantCount,
                formattedComments
        );
    }

    public TicketDetailResponse getTicketDetail(String ticketNumber, Integer actorUserId) {
        Ticket ticket = getByTicketNumber(ticketNumber, actorUserId);

        String categoryName = categoryDAO.findById(ticket.getCategoryId())
                .map(Category::getCategoryName)
                .orElse("Unknown");

        String creatorName = userDAO.findById(ticket.getCreatedBy())
                .map(User::getName)
                .orElse("Unknown");

        String agentName = null;
        if (ticket.getAssignedTo() != null) {
            agentName = userDAO.findById(ticket.getAssignedTo())
                    .map(User::getName)
                    .orElse(null);
        }

        String incidentTitle = null;
        if (ticket.getIncidentId() != null) {
            incidentTitle = incidentDAO.findById(ticket.getIncidentId())
                    .map(Incident::getTitle)
                    .orElse(null);
        }

        int participantCount = ticketParticipantDAO.findByTicketId(ticket.getTicketId()).size();

        List<Comment> rawComments = commentDAO.findByTicketId(ticket.getTicketId());
        List<CommentDetailResponse> commentResponses = new ArrayList<>();
        for (Comment c : rawComments) {
            String author = userDAO.findById(c.getUserId())
                    .map(User::getName)
                    .orElse("User " + c.getUserId());
            commentResponses.add(new CommentDetailResponse(
                    c.getCommentId(),
                    c.getTicketId(),
                    c.getUserId(),
                    author,
                    c.getCommentText(),
                    c.getCreatedAt()
            ));
        }

        UrgencyResult urgency = urgencyService.evaluate(ticketNumber, actorUserId);

        return new TicketDetailResponse(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                categoryName,
                ticket.getPriority(),
                urgency.level().name(),
                urgency.score(),
                urgency.overdue(),
                urgency.dueAt(),
                urgency.reasons(),
                ticket.getStatus(),
                creatorName,
                creatorName,
                agentName,
                incidentTitle,
                participantCount,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                commentResponses
        );
    }

    public Ticket getByTicketNumber(String ticketNumber, Integer actorUserId) {
        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket number cannot be blank");
        }
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }

        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        Ticket ticket = ticketDAO.findByTicketNumber(ticketNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketNumber));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(actor.getRole());
        boolean isCreator = actorUserId.equals(ticket.getCreatedBy());
        boolean isParticipant = ticketParticipantDAO.isParticipant(ticket.getTicketId(), actorUserId);
        boolean isAssignedAgent = "AGENT".equalsIgnoreCase(actor.getRole()) && actorUserId.equals(ticket.getAssignedTo());

        if (!isAdmin && !isCreator && !isParticipant && !isAssignedAgent) {
            throw new SecurityException("Access denied for ticket: " + ticketNumber);
        }
        return ticket;
    }

    public List<Comment> getComments(Integer ticketId, Integer actorUserId) {
        if (ticketId == null || actorUserId == null) {
            throw new IllegalArgumentException("Ticket ID and actor user ID cannot be null");
        }
        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));
        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(actor.getRole());
        boolean isCreator = actorUserId.equals(ticket.getCreatedBy());
        boolean isParticipant = ticketParticipantDAO.isParticipant(ticketId, actorUserId);
        boolean isAssignedAgent = "AGENT".equalsIgnoreCase(actor.getRole()) && actorUserId.equals(ticket.getAssignedTo());

        if (!isAdmin && !isCreator && !isParticipant && !isAssignedAgent) {
            throw new SecurityException("Access denied to view comments for ticket: " + ticketId);
        }
        return commentDAO.findByTicketId(ticketId);
    }
}
