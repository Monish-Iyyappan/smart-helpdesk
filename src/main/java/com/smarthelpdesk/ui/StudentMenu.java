package com.smarthelpdesk.ui;

import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.model.Category;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.service.TicketDetail;
import com.smarthelpdesk.service.TicketService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Menu interface for student users.
 * Provides ticket creation, tracking, joining, and commenting.
 */
@Component
public class StudentMenu {

    private final TicketService ticketService;
    private final ConsoleIO io;

    public StudentMenu(TicketService ticketService, ConsoleIO io) {
        this.ticketService = ticketService;
        this.io = io;
    }

    public void show(Session session) {
        User user = session.getCurrentUser();
        boolean back = false;

        while (!back) {
            try {
                io.printHeader("STUDENT MENU - " + user.getName());
                System.out.println("1. Create ticket");
                System.out.println("2. My tickets");
                System.out.println("3. View ticket by number");
                System.out.println("4. Join ticket by number");
                System.out.println("5. Add comment");
                System.out.println("6. Logout");
                io.printDivider();

                int choice = io.readInt("Select an option (1-6, 0 to back): ", 0, 6);
                switch (choice) {
                    case 1 -> createTicket(user);
                    case 2 -> myTickets(user);
                    case 3 -> viewTicket(user);
                    case 4 -> joinTicket(user);
                    case 5 -> addComment(user);
                    case 6, 0 -> {
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

    private void createTicket(User user) {
        List<Category> categories = ticketService.listCategories();
        if (categories.isEmpty()) {
            System.out.println("No categories available to create ticket.");
            return;
        }

        io.printDivider();
        System.out.println("Select Category:");
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("%d. %s%n", (i + 1), categories.get(i).getCategoryName());
        }
        int catChoice = io.readInt(String.format("Enter category (1-%d, 0 to cancel): ", categories.size()), 0, categories.size());
        if (catChoice == 0) {
            System.out.println("Ticket creation cancelled.");
            return;
        }
        Category selectedCategory = categories.get(catChoice - 1);

        String title = io.readLine("Enter ticket title (0 to cancel): ");
        if (title.equals("0") || title.isBlank()) {
            System.out.println("Ticket creation cancelled.");
            return;
        }

        String description = io.readLine("Enter ticket description (0 to cancel): ");
        if (description.equals("0") || description.isBlank()) {
            System.out.println("Ticket creation cancelled.");
            return;
        }

        System.out.println("Select Priority:");
        System.out.println("1. LOW");
        System.out.println("2. MEDIUM");
        System.out.println("3. HIGH");
        System.out.println("4. URGENT");
        int priorityChoice = io.readInt("Enter priority (1-4, 0 to cancel): ", 0, 4);
        if (priorityChoice == 0) {
            System.out.println("Ticket creation cancelled.");
            return;
        }
        String priority = switch (priorityChoice) {
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            case 4 -> "URGENT";
            default -> "LOW";
        };

        io.printDivider();
        System.out.println("Ticket Summary:");
        System.out.println("Category   : " + selectedCategory.getCategoryName());
        System.out.println("Title      : " + title);
        System.out.println("Description: " + description);
        System.out.println("Priority   : " + priority);
        io.printDivider();

        if (!io.confirm("Confirm ticket submission? (y/n): ")) {
            System.out.println("Ticket creation cancelled.");
            return;
        }

        Ticket created = ticketService.create(user.getUserId(), selectedCategory.getCategoryId(), title, description, priority);
        System.out.println("Ticket created successfully! Ticket Number: " + created.getTicketNumber());
    }

    private void myTickets(User user) {
        List<TicketListResponse> tickets = ticketService.myTicketResponses(user.getUserId());
        io.printHeader("MY TICKETS");
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

    private void joinTicket(User user) {
        String ticketNum = io.readLine("Enter ticket number to join (e.g. HD-1001, 0 to cancel): ");
        if (ticketNum.equals("0") || ticketNum.isBlank()) {
            return;
        }
        boolean joined = ticketService.join(ticketNum, user.getUserId());
        if (joined) {
            System.out.println("Successfully joined ticket: " + ticketNum);
        } else {
            System.out.println("You are already a participant of ticket: " + ticketNum);
        }
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
