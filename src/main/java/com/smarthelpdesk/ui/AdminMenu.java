package com.smarthelpdesk.ui;

import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.priority.UrgencyService;
import com.smarthelpdesk.service.AdminService;
import com.smarthelpdesk.service.DashboardService;
import com.smarthelpdesk.service.IncidentService;
import com.smarthelpdesk.service.TicketService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Menu interface for administrator users.
 * Provides system dashboards, ticket assignment, incident management, and agent creation.
 */
@Component
public class AdminMenu {

    private final AdminService adminService;
    private final TicketService ticketService;
    private final IncidentService incidentService;
    private final DashboardService dashboardService;
    private final UrgencyService urgencyService;
    private final ConsoleIO io;

    public AdminMenu(AdminService adminService, TicketService ticketService,
                     IncidentService incidentService, DashboardService dashboardService,
                     UrgencyService urgencyService, ConsoleIO io) {
        this.adminService = adminService;
        this.ticketService = ticketService;
        this.incidentService = incidentService;
        this.dashboardService = dashboardService;
        this.urgencyService = urgencyService;
        this.io = io;
    }

    public void show(Session session) {
        User user = session.getCurrentUser();
        boolean back = false;

        while (!back) {
            try {
                io.printHeader("ADMIN MENU - " + user.getName());
                System.out.println("1.  Dashboard summary");
                System.out.println("2.  All tickets");
                System.out.println("3.  Unassigned tickets");
                System.out.println("4.  Overdue tickets");
                System.out.println("5.  Assign ticket");
                System.out.println("6.  Close ticket");
                System.out.println("7.  Reopen ticket");
                System.out.println("8.  Create incident");
                System.out.println("9.  Link ticket to incident");
                System.out.println("10. View incidents with affected count");
                System.out.println("11. Add agent account");
                System.out.println("12. Logout");
                io.printDivider();

                int choice = io.readInt("Select an option (1-12, 0 to back): ", 0, 12);
                switch (choice) {
                    case 1 -> dashboardSummary();
                    case 2 -> allTickets(user);
                    case 3 -> unassignedTickets(user);
                    case 4 -> overdueTickets(user);
                    case 5 -> assignTicket(user);
                    case 6 -> closeTicket(user);
                    case 7 -> reopenTicket(user);
                    case 8 -> createIncident(user);
                    case 9 -> linkTicket(user);
                    case 10 -> viewIncidents();
                    case 11 -> addAgent(user);
                    case 12, 0 -> {
                        System.out.println("Logging out...");
                        back = true;
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (DataAccessException ex) {
                String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
                System.out.println("Error: Database error - " + (msg != null ? msg : "unable to perform operation"));
            }
        }
    }

    private void dashboardSummary() {
        String dashboard = dashboardService.buildDashboard();
        System.out.println(dashboard);
    }

    private void allTickets(User user) {
        System.out.println("Sort options:");
        System.out.println("1. Default (Created date)");
        System.out.println("2. Urgency level");
        int sortChoice = io.readInt("Select sort (1-2): ", 1, 2);
        String sort = (sortChoice == 2) ? "urgency" : null;

        List<TicketListResponse> tickets = adminService.viewAllWithUrgency(user.getUserId(), sort);
        io.printHeader("ALL TICKETS");
        io.printTicketList(tickets);
    }

    private void unassignedTickets(User user) {
        List<TicketListResponse> tickets = adminService.viewUnassignedWithUrgency(user.getUserId());
        io.printHeader("UNASSIGNED TICKETS");
        io.printTicketList(tickets);
    }

    private void overdueTickets(User user) {
        List<TicketListResponse> tickets = urgencyService.listOverdue(user.getUserId());
        io.printHeader("OVERDUE TICKETS");
        io.printTicketList(tickets);
    }

    private void assignTicket(User user) {
        List<Ticket> unassigned = adminService.viewUnassigned(user.getUserId());
        if (unassigned.isEmpty()) {
            System.out.println("No unassigned tickets found.");
            return;
        }

        io.printDivider();
        System.out.println("Unassigned Tickets:");
        for (int i = 0; i < unassigned.size(); i++) {
            Ticket t = unassigned.get(i);
            System.out.printf("%d. %s - %s (%s)%n", (i + 1), t.getTicketNumber(), io.truncate(t.getTitle(), 22), t.getPriority());
        }
        int ticketChoice = io.readInt(String.format("Select ticket (1-%d, 0 to cancel): ", unassigned.size()), 0, unassigned.size());
        if (ticketChoice == 0) {
            System.out.println("Assignment cancelled.");
            return;
        }
        Ticket selectedTicket = unassigned.get(ticketChoice - 1);

        List<User> agents = adminService.listAgents(user.getUserId());
        if (agents.isEmpty()) {
            System.out.println("No agents found in the system.");
            return;
        }

        System.out.println("Available Agents:");
        for (int i = 0; i < agents.size(); i++) {
            User a = agents.get(i);
            System.out.printf("%d. %s (%s)%n", (i + 1), a.getName(), a.getEmail());
        }
        int agentChoice = io.readInt(String.format("Select agent (1-%d, 0 to cancel): ", agents.size()), 0, agents.size());
        if (agentChoice == 0) {
            System.out.println("Assignment cancelled.");
            return;
        }
        User selectedAgent = agents.get(agentChoice - 1);

        if (!io.confirm(String.format("Assign ticket %s to %s? (y/n): ", selectedTicket.getTicketNumber(), selectedAgent.getName()))) {
            System.out.println("Assignment cancelled.");
            return;
        }

        adminService.assign(selectedTicket.getTicketId(), user.getUserId(), selectedAgent.getUserId());
        System.out.println("Ticket assigned successfully.");
    }

    private void closeTicket(User user) {
        String ticketNum = io.readLine("Enter ticket number to close (0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        Ticket ticket = ticketService.getByTicketNumber(ticketNum, user.getUserId());
        String note = io.readLine("Enter closure note (10-500 chars): ");
        if (note == null || note.trim().length() < 10 || note.trim().length() > 500) {
            System.out.println("Closure note must be between 10 and 500 characters.");
            return;
        }
        if (!io.confirm("Are you sure you want to CLOSE ticket " + ticketNum + "? (y/n): ")) {
            System.out.println("Operation cancelled.");
            return;
        }
        adminService.close(ticket.getTicketId(), user.getUserId(), note.trim());
        System.out.println("Ticket closed successfully.");
    }

    private void reopenTicket(User user) {
        String ticketNum = io.readLine("Enter ticket number to reopen (0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        Ticket ticket = ticketService.getByTicketNumber(ticketNum, user.getUserId());
        if (!io.confirm("Are you sure you want to REOPEN ticket " + ticketNum + "? (y/n): ")) {
            System.out.println("Operation cancelled.");
            return;
        }
        adminService.reopen(ticket.getTicketId(), user.getUserId());
        System.out.println("Ticket reopened to IN_PROGRESS successfully.");
    }

    private void createIncident(User user) {
        String title = io.readLine("Enter incident title (0 to cancel): ");
        if (title.equals("0") || title.isBlank()) {
            return;
        }
        String description = io.readLine("Enter incident description (0 to cancel): ");
        if (description.equals("0") || description.isBlank()) {
            return;
        }
        String location = io.readLine("Enter incident location (0 to cancel): ");
        if (location.equals("0") || location.isBlank()) {
            return;
        }

        System.out.println("Select Severity:");
        System.out.println("1. LOW");
        System.out.println("2. MEDIUM");
        System.out.println("3. HIGH");
        System.out.println("4. CRITICAL");
        int sevChoice = io.readInt("Enter severity (1-4, 0 to cancel): ", 0, 4);
        if (sevChoice == 0) {
            System.out.println("Incident creation cancelled.");
            return;
        }
        String severity = switch (sevChoice) {
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            case 4 -> "CRITICAL";
            default -> "LOW";
        };

        if (!io.confirm("Confirm incident creation? (y/n): ")) {
            System.out.println("Incident creation cancelled.");
            return;
        }

        Incident inc = incidentService.create(user.getUserId(), title, description, location, severity);
        System.out.println("Incident created successfully! Incident ID: " + inc.getIncidentId());
    }

    private void linkTicket(User user) {
        String ticketNum = io.readLine("Enter ticket number to link (0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        Ticket ticket = ticketService.getByTicketNumber(ticketNum, user.getUserId());

        List<Incident> incidents = incidentService.listAll();
        if (incidents.isEmpty()) {
            System.out.println("No incidents available to link.");
            return;
        }

        System.out.println("Available Incidents:");
        for (int i = 0; i < incidents.size(); i++) {
            Incident inc = incidents.get(i);
            System.out.printf("%d. [ID: %d] %s (Severity: %s, Status: %s)%n",
                    (i + 1), inc.getIncidentId(), io.truncate(inc.getTitle(), 25), inc.getSeverity(), inc.getStatus());
        }
        int incChoice = io.readInt(String.format("Select incident (1-%d, 0 to cancel): ", incidents.size()), 0, incidents.size());
        if (incChoice == 0) {
            System.out.println("Operation cancelled.");
            return;
        }
        Incident selectedIncident = incidents.get(incChoice - 1);

        if (!io.confirm(String.format("Link ticket %s to incident '%s'? (y/n): ", ticket.getTicketNumber(), selectedIncident.getTitle()))) {
            System.out.println("Operation cancelled.");
            return;
        }

        incidentService.linkTicket(user.getUserId(), ticket.getTicketId(), selectedIncident.getIncidentId());
        System.out.println("Ticket linked to incident successfully.");
    }

    private void viewIncidents() {
        List<Incident> incidents = incidentService.listAll();
        io.printHeader("CAMPUS INCIDENTS");
        if (incidents.isEmpty()) {
            System.out.println("No incidents found");
            return;
        }

        System.out.printf("%-4s %-20s %-10s %-10s %-8s%n", "ID", "TITLE", "SEVERITY", "STATUS", "AFFECTED");
        io.printDivider();
        for (Incident inc : incidents) {
            int affected = incidentService.affectedStudents(inc.getIncidentId());
            System.out.printf("%-4d %-20s %-10s %-10s %-8d%n",
                    inc.getIncidentId(),
                    io.truncate(inc.getTitle(), 20),
                    inc.getSeverity(),
                    inc.getStatus(),
                    affected);
        }
        io.printDoubleDivider();
    }

    private void addAgent(User user) {
        String name = io.readLine("Enter agent name (0 to cancel): ");
        if (name.equals("0") || name.isBlank()) {
            return;
        }
        String email = io.readLine("Enter agent email (0 to cancel): ");
        if (email.equals("0") || email.isBlank()) {
            return;
        }
        String password = io.readPassword("Enter password for new agent (min 8 chars): ");
        String confirmPass = io.readPassword("Confirm password: ");
        if (!password.equals(confirmPass)) {
            System.out.println("Error: Passwords do not match.");
            return;
        }

        adminService.createAgent(user.getUserId(), name, email, password);
        System.out.println("Agent account created successfully!");
    }
}
