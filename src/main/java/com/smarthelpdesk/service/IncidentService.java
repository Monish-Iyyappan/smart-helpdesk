package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.IncidentDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.dto.IncidentTicketResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.exception.ResourceNotFoundException;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.priority.UrgencyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private final IncidentDAO incidentDAO;
    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;
    private final UrgencyService urgencyService;

    public IncidentService(IncidentDAO incidentDAO, TicketDAO ticketDAO, UserDAO userDAO, UrgencyService urgencyService) {
        this.incidentDAO = incidentDAO;
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
        this.urgencyService = urgencyService;
    }

    private User checkAdmin(Integer adminUserId) {
        if (adminUserId == null) {
            throw new IllegalArgumentException("Admin user ID cannot be null");
        }
        User user = userDAO.findById(adminUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + adminUserId));
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Action requires ADMIN role");
        }
        return user;
    }

    public Incident create(Integer adminUserId, String title, String description, String location, String severity) {
        checkAdmin(adminUserId);

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity cannot be null");
        }

        String normSeverity = severity.trim().toUpperCase();
        if (!normSeverity.equals("LOW") && !normSeverity.equals("MEDIUM") &&
            !normSeverity.equals("HIGH") && !normSeverity.equals("CRITICAL")) {
            throw new IllegalArgumentException("Invalid severity. Must be LOW, MEDIUM, HIGH, or CRITICAL");
        }

        Incident incident = new Incident(title.trim(), description.trim(), location.trim(), "OPEN", normSeverity);
        Integer incidentId = incidentDAO.insert(incident);
        incident.setIncidentId(incidentId);
        return incident;
    }

    public void linkTicket(Integer adminUserId, Integer ticketId, Integer incidentId) {
        checkAdmin(adminUserId);

        if (ticketId == null || incidentId == null) {
            throw new IllegalArgumentException("Ticket ID and incident ID cannot be null");
        }

        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("CLOSED tickets cannot be modified or linked to incidents");
        }

        incidentDAO.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        ticketDAO.linkIncident(ticketId, incidentId);
    }

    public Incident getById(Integer incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("Incident ID cannot be null");
        }
        return incidentDAO.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
    }

    public List<Incident> listAll() {
        return incidentDAO.findAll();
    }

    public List<IncidentResponse> listAllWithCounts() {
        return incidentDAO.findAllWithCounts();
    }

    public int affectedStudents(Integer incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("Incident ID cannot be null");
        }
        incidentDAO.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
        return incidentDAO.countAffectedStudents(incidentId);
    }

    public List<Ticket> getTicketsByIncident(Integer incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("Incident ID cannot be null");
        }
        incidentDAO.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
        return ticketDAO.findByIncidentId(incidentId);
    }

    public List<IncidentTicketResponse> getTicketsByIncident(Integer incidentId, Integer actorUserId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("Incident ID cannot be null");
        }
        if (actorUserId == null) {
            throw new IllegalArgumentException("Actor user ID cannot be null");
        }

        incidentDAO.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));

        User actor = userDAO.findById(actorUserId)
                .orElseThrow(() -> new SecurityException("User not found: " + actorUserId));

        List<Ticket> tickets;
        if ("ADMIN".equalsIgnoreCase(actor.getRole())) {
            tickets = ticketDAO.findByIncidentId(incidentId);
        } else if ("AGENT".equalsIgnoreCase(actor.getRole())) {
            tickets = ticketDAO.findByIncidentIdAndAssignedTo(incidentId, actorUserId);
        } else if ("STUDENT".equalsIgnoreCase(actor.getRole())) {
            tickets = ticketDAO.findByIncidentIdAndParticipant(incidentId, actorUserId);
        } else {
            throw new SecurityException("Unknown user role: " + actor.getRole());
        }

        List<TicketListResponse> evaluatedList = urgencyService.evaluateTicketsBatch(tickets);

        Map<Integer, String> userNames = userDAO.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, User::getName, (a, b) -> a));

        return evaluatedList.stream().map(t -> {
            String agentName = t.assignedTo() != null ? userNames.get(t.assignedTo()) : null;
            return new IncidentTicketResponse(
                    t.ticketId(),
                    t.ticketId(),
                    t.ticketNumber(),
                    t.title(),
                    t.description(),
                    t.status(),
                    t.requestedPriority(),
                    t.requestedPriority(),
                    t.categoryName(),
                    t.categoryName(),
                    agentName,
                    agentName,
                    t.assignedTo(),
                    t.createdAt(),
                    t.createdAt(),
                    t.overdue(),
                    t.urgencyLevel(),
                    t.urgencyScore(),
                    t.incidentId()
            );
        }).toList();
    }
}
