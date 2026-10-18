package com.smarthelpdesk.ui;

import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.service.TicketDetail;
import com.smarthelpdesk.service.TicketService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Menu interface for support agent users.
 * Allows viewing assigned tickets, progressing ticket status, and commenting.
 */
@Component
public class AgentMenu {

    private final TicketService ticketService;
    private final ConsoleIO io;

    public AgentMenu(TicketService ticketService, ConsoleIO io) {
        this.ticketService = ticketService;
        this.io = io;
    }

    public void show(Session session) {
        User user = session.getCurrentUser();
        boolean back = false;

        while (!back) {
            try {
                io.printHeader("AGENT MENU - " + user.getName());
                System.out.println("1. My assigned tickets");
                System.out.println("2. View ticket");
                System.out.println("3. Update status");
                System.out.println("4. Add comment");
                System.out.println("5. Logout");
                io.printDivider();

                int choice = io.readInt("Select an option (1-5, 0 to back): ", 0, 5);
                switch (choice) {
                    case 1 -> myAssignedTickets(user);
                    case 2 -> viewTicket(user);
                    case 3 -> updateStatus(user);
                    case 4 -> addComment(user);
                    case 5, 0 -> {
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

    private void myAssignedTickets(User user) {
        List<TicketListResponse> tickets = ticketService.myTicketResponses(user.getUserId());
        io.printHeader("MY ASSIGNED TICKETS");
        io.printTicketList(tickets);
    }

    private void viewTicket(User user) {
        String ticketNum = io.readLine("Enter ticket number (e.g. HD-1001, 0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        TicketDetail detail = ticketService.getDetail(ticketNum, user.getUserId());
        io.printTicketDetail(detail);
    }

    private void updateStatus(User user) {
        String ticketNum = io.readLine("Enter ticket number to update status (0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        Ticket ticket = ticketService.getByTicketNumber(ticketNum, user.getUserId());
        String currentStatus = ticket.getStatus();

        String nextStatus;
        if ("ASSIGNED".equalsIgnoreCase(currentStatus)) {
            nextStatus = "IN_PROGRESS";
        } else if ("IN_PROGRESS".equalsIgnoreCase(currentStatus)) {
            nextStatus = "RESOLVED";
        } else {
            System.out.println("Error: Cannot update status. Current status is " + currentStatus +
                    " (only ASSIGNED -> IN_PROGRESS and IN_PROGRESS -> RESOLVED allowed).");
            return;
        }

        System.out.printf("Current status: %s. Valid next status: %s%n", currentStatus, nextStatus);
        if (!io.confirm("Update status to " + nextStatus + "? (y/n): ")) {
            System.out.println("Status update cancelled.");
            return;
        }

        ticketService.updateStatus(ticket.getTicketId(), user.getUserId(), nextStatus);
        System.out.println("Ticket status updated to " + nextStatus + " successfully.");
    }

    private void addComment(User user) {
        String ticketNum = io.readLine("Enter ticket number (0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        Ticket ticket = ticketService.getByTicketNumber(ticketNum, user.getUserId());
        String text = io.readLine("Enter comment text (0 to cancel): ");
        if (text.equals("0") || text.isBlank()) {
            System.out.println("Comment cancelled.");
            return;
        }
        ticketService.addComment(ticket.getTicketId(), user.getUserId(), text);
        System.out.println("Comment added successfully.");
    }
}
