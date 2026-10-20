package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.DashboardDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.dto.AdminStatsResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UrgencySummaryResponse;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.assignment.AssignmentService;
import com.smarthelpdesk.priority.UrgencyService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final TicketDAO ticketDAO;
    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final DashboardDAO dashboardDAO;
    private final UrgencyService urgencyService;
    private final AssignmentService assignmentService;

    public AdminService(TicketDAO ticketDAO, UserDAO userDAO,
                        PasswordEncoder passwordEncoder, DashboardDAO dashboardDAO,
                        UrgencyService urgencyService, AssignmentService assignmentService) {
        this.ticketDAO = ticketDAO;
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.dashboardDAO = dashboardDAO;
        this.urgencyService = urgencyService;
        this.assignmentService = assignmentService;
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

    public Ticket assign(Integer ticketId, Integer adminUserId, Integer agentId) {
        return assignmentService.assignTicket(ticketId, adminUserId, agentId, null);
    }

    public Ticket assign(Integer ticketId, Integer adminUserId, Integer agentId, String reason) {
        return assignmentService.assignTicket(ticketId, adminUserId, agentId, reason);
    }

    public Ticket close(Integer ticketId, Integer adminUserId) {
        checkAdmin(adminUserId);

        if (ticketId == null) {
            throw new IllegalArgumentException("Ticket ID cannot be null");
        }

        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("Ticket is already CLOSED");
        }

        if (!"RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("Can only close a ticket from RESOLVED status. Current status: " + ticket.getStatus());
        }

        ticketDAO.updateStatus(ticketId, "CLOSED");
        ticket.setStatus("CLOSED");
        return ticket;
    }

    public Ticket reopen(Integer ticketId, Integer adminUserId) {
        checkAdmin(adminUserId);

        if (ticketId == null) {
            throw new IllegalArgumentException("Ticket ID cannot be null");
        }

        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("CLOSED tickets cannot be reopened");
        }

        if (!"RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            throw new IllegalStateException("Can only reopen a ticket from RESOLVED status. Current status: " + ticket.getStatus());
        }

        ticketDAO.updateStatus(ticketId, "IN_PROGRESS");
        ticket.setStatus("IN_PROGRESS");
        return ticket;
    }

    public List<Ticket> viewAll(Integer adminUserId) {
        checkAdmin(adminUserId);
        return ticketDAO.findAll();
    }

    public List<TicketListResponse> viewAllWithUrgency(Integer adminUserId, String sort) {
        checkAdmin(adminUserId);
        List<TicketListResponse> tickets = urgencyService.evaluateAll(adminUserId);
        if ("urgency".equalsIgnoreCase(sort)) {
            return tickets;
        }
        List<TicketListResponse> list = new ArrayList<>(tickets);
        list.sort(Comparator.comparing(TicketListResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TicketListResponse::ticketId, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    public List<Ticket> viewUnassigned(Integer adminUserId) {
        checkAdmin(adminUserId);
        return ticketDAO.findByStatus("OPEN");
    }

    public List<TicketListResponse> viewUnassignedWithUrgency(Integer adminUserId) {
        checkAdmin(adminUserId);
        return viewAllWithUrgency(adminUserId, null).stream()
                .filter(t -> "OPEN".equalsIgnoreCase(t.status()))
                .toList();
    }

    public List<User> listAgents(Integer adminUserId) {
        checkAdmin(adminUserId);
        return userDAO.findByRole("AGENT");
    }

    public User createAgent(Integer adminUserId, String name, String email, String rawPassword) {
        checkAdmin(adminUserId);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        String cleanEmail = email.trim();
        if (!cleanEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + cleanEmail);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        User agent = new User(name.trim(), cleanEmail, passwordHash, "AGENT");
        userDAO.insert(agent);
        return agent;
    }

    public AdminStatsResponse getStats(Integer adminUserId) {
        checkAdmin(adminUserId);
        Map<String, Integer> usersByRole = dashboardDAO.getUserCountsByRole();
        Map<String, Integer> countsByStatus = dashboardDAO.getTicketCountsByStatus();
        Map<String, Integer> countsByPriority = dashboardDAO.getTicketCountsByPriority();
        int totalTickets = countsByStatus.values().stream().mapToInt(Integer::intValue).sum();
        int totalIncidents = dashboardDAO.getIncidentCount();
        UrgencySummaryResponse urgencySummary = urgencyService.summary(adminUserId);

        return new AdminStatsResponse(
                totalTickets,
                countsByStatus,
                countsByPriority,
                usersByRole,
                totalIncidents,
                urgencySummary.totalOverdue(),
                urgencySummary.countsByLevel()
        );
    }
}
