package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.IncidentDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentDAO incidentDAO;
    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;

    public IncidentService(IncidentDAO incidentDAO, TicketDAO ticketDAO, UserDAO userDAO) {
        this.incidentDAO = incidentDAO;
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
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
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
    }

    public List<Incident> listAll() {
        return incidentDAO.findAll();
    }

    public int affectedStudents(Integer incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("Incident ID cannot be null");
        }
        incidentDAO.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
        return incidentDAO.countAffectedStudents(incidentId);
    }

    public List<Ticket> getTicketsByIncident(Integer incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("Incident ID cannot be null");
        }
        incidentDAO.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
        return ticketDAO.findByIncidentId(incidentId);
    }
}
