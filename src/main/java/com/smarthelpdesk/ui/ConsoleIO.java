package com.smarthelpdesk.ui;

import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.service.TicketDetail;
import org.springframework.stereotype.Component;

import java.io.Console;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Shared terminal I/O helper for formatting and user input.
 * Formats menus, boxes, and tables strictly within 52 characters ASCII.
 */
@Component
public class ConsoleIO {

    public static final int WIDTH = 52;
    public static final String LINE_EQUALS = "====================================================";
    public static final String LINE_DASHES = "----------------------------------------------------";

    private final Scanner scanner;

    public ConsoleIO() {
        this.scanner = new Scanner(System.in);
    }

    public void printHeader(String title) {
        System.out.println(LINE_EQUALS);
        System.out.println(center(title, WIDTH));
        System.out.println(LINE_EQUALS);
    }

    public void printDivider() {
        System.out.println(LINE_DASHES);
    }

    public void printDoubleDivider() {
        System.out.println(LINE_EQUALS);
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.printf("Invalid number. Please enter between %d and %d.%n", min, max);
            }
        }
    }

    public String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            return chars != null ? new String(chars).trim() : "";
        } else {
            System.out.println("Warning: Console not available, password typing is visible.");
            System.out.print(prompt);
            return scanner.nextLine().trim();
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("Please enter 'y' or 'n'.");
        }
    }

    public String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        if (maxLen <= 3) {
            return text.substring(0, maxLen);
        }
        return text.substring(0, maxLen - 3) + "...";
    }

    public String center(String text, int width) {
        if (text == null) {
            return "";
        }
        int padding = Math.max(0, (width - text.length()) / 2);
        int remaining = Math.max(0, width - text.length() - padding);
        return " ".repeat(padding) + text + " ".repeat(remaining);
    }

    public void printTicketsTable(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            System.out.println("No tickets found");
            return;
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        System.out.printf("%-10s %-22s %-8s %-11s %-10s%n", "TICKET NO", "TITLE", "PRIORITY", "STATUS", "CREATED");
        printDivider();
        for (Ticket t : tickets) {
            String num = t.getTicketNumber() != null ? t.getTicketNumber() : "-";
            String title = truncate(t.getTitle(), 22);
            String priority = t.getPriority() != null ? t.getPriority() : "-";
            String status = t.getStatus() != null ? t.getStatus() : "-";
            String created = t.getCreatedAt() != null ? t.getCreatedAt().format(df) : "-";
            System.out.printf("%-10s %-22s %-8s %-11s %-10s%n", num, title, priority, status, created);
        }
        printDivider();
    }

    public void printTicketList(List<TicketListResponse> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            System.out.println("No tickets found");
            return;
        }
        System.out.printf("%-10s %-18s %-8s %-10s %-11s %-7s%n", "TICKET NO", "TITLE", "PRIORITY", "URGENCY", "STATUS", "OVERDUE");
        printDivider();
        for (TicketListResponse t : tickets) {
            String num = t.ticketNumber() != null ? t.ticketNumber() : "-";
            String title = truncate(t.title(), 18);
            String priority = t.requestedPriority() != null ? t.requestedPriority() : "-";
            String urgency = t.urgencyLevel() != null ? t.urgencyLevel() : "-";
            String status = t.status() != null ? t.status() : "-";
            String overdueMarker = t.overdue() ? "[!]" : "-";
            System.out.printf("%-10s %-18s %-8s %-10s %-11s %-7s%n", num, title, priority, urgency, status, overdueMarker);
        }
        printDivider();
    }

    public void printTicketDetail(TicketDetail detail) {
        printDoubleDivider();
        System.out.println(center("TICKET DETAILS: " + detail.ticketNumber(), WIDTH));
        printDoubleDivider();
        System.out.printf("Title        : %s%n", detail.title());
        System.out.printf("Category     : %s%n", detail.categoryName());
        System.out.printf("Requested Pri: %s%n", detail.requestedPriority());
        System.out.printf("Urgency Level: %s (Score: %d)%s%n",
                detail.urgencyLevel(),
                detail.urgencyScore(),
                detail.overdue() ? " [OVERDUE]" : "");
        if (detail.dueAt() != null) {
            System.out.printf("Due At       : %s%n", detail.dueAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        System.out.printf("Status       : %s%n", detail.status());
        System.out.printf("Created By   : %s%n", detail.createdByName());
        System.out.printf("Assigned To  : %s%n", detail.assignedAgentName());
        if (detail.incidentTitle() != null && !detail.incidentTitle().isBlank()) {
            System.out.printf("Incident     : %s%n", detail.incidentTitle());
        }
        System.out.printf("Participants : %d%n", detail.participantCount());
        printDivider();
        System.out.println("Urgency Reasons:");
        if (detail.reasons() == null || detail.reasons().isEmpty()) {
            System.out.println("  (None)");
        } else {
            for (String r : detail.reasons()) {
                System.out.println("  * " + r);
            }
        }
        printDivider();
        System.out.println("Description:");
        System.out.println(detail.description());
        printDivider();
        System.out.println("Comments:");
        if (detail.comments() == null || detail.comments().isEmpty()) {
            System.out.println("  (No comments yet)");
        } else {
            for (String c : detail.comments()) {
                System.out.println("  " + c);
            }
        }
        printDoubleDivider();
    }
}
