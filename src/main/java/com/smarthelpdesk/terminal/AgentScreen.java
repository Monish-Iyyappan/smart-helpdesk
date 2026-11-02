package com.smarthelpdesk.terminal;

import com.smarthelpdesk.dto.CommentDetailResponse;
import com.smarthelpdesk.dto.TicketDetailResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Screen 3: Agent Workspace.
 * Displays agent queue in stacked sections (ASSIGNED, IN PROGRESS, RESOLVED),
 * urgency factors, status advancement (Start Work, Resolve), and ticket detail modal.
 */
@Component
public class AgentScreen {

    public enum AgentCommand {
        START_WORK, DONE_RESOLVE, TICKET_DETAILS, COMMENT, OVERDUE_TOGGLE, REFRESH, QUIT, UNKNOWN
    }

    private final Ui ui;
    private final ApiClient apiClient;
    private final CommentFlow commentFlow;

    public AgentScreen(Ui ui, ApiClient apiClient) {
        this.ui = ui;
        this.apiClient = apiClient;
        this.commentFlow = new CommentFlow(apiClient, ui);
    }

    public void show(TerminalSession session) {
        if (!"AGENT".equalsIgnoreCase(session.getRole())) {
            ui.printError("Access denied: Agent screen requires AGENT role. Current role: " + session.getRole());
            return;
        }

        List<TicketListResponse> allTickets = loadTickets(session.authHeader());
        boolean overdueOnly = false;
        boolean redrawScreen = true;

        while (true) {
            if (redrawScreen) {
                renderScreen(session.user(), allTickets, overdueOnly);
            }
            redrawScreen = false;

            String input = ui.readLine("Enter your choice (S / D / T / M / O / R / Q): ").trim();
            AgentCommand cmd = parseCommand(input);

            switch (cmd) {
                case START_WORK -> {
                    boolean success = handleStartWork(allTickets, session.authHeader());
                    if (success) {
                        allTickets = loadTickets(session.authHeader());
                        redrawScreen = true;
                    } else {
                        printControls();
                    }
                }
                case DONE_RESOLVE -> {
                    boolean success = handleResolveTicket(allTickets, session.authHeader());
                    if (success) {
                        allTickets = loadTickets(session.authHeader());
                        redrawScreen = true;
                    } else {
                        printControls();
                    }
                }
                case TICKET_DETAILS -> {
                    boolean success = handleViewTicket(session.authHeader());
                    if (success) {
                        redrawScreen = true;
                    } else {
                        printControls();
                    }
                }
                case COMMENT -> {
                    boolean updated = handleComment(session.authHeader());
                    if (updated) {
                        allTickets = loadTickets(session.authHeader());
                        redrawScreen = true;
                    } else {
                        printControls();
                    }
                }
                case OVERDUE_TOGGLE -> {
                    overdueOnly = !overdueOnly;
                    redrawScreen = true;
                }
                case REFRESH -> {
                    overdueOnly = false;
                    allTickets = loadTickets(session.authHeader());
                    ui.printSuccess("Queue refreshed and filters reset.");
                    redrawScreen = true;
                }
                case QUIT -> {
                    System.out.println("Logging out...");
                    return;
                }
                case UNKNOWN -> {
                    ui.printError("Invalid choice. Type start, done, ticket, comment, overdue, refresh, or quit.");
                    printControls();
                }
            }
        }
    }

    /**
     * Maps user input to an AgentCommand.
     */
    private AgentCommand parseCommand(String input) {
        if (input == null) return AgentCommand.UNKNOWN;
        String clean = input.trim().toLowerCase();
        return switch (clean) {
            case "s", "start" -> AgentCommand.START_WORK;
            case "d", "done", "resolve" -> AgentCommand.DONE_RESOLVE;
            case "t", "ticket", "details", "view" -> AgentCommand.TICKET_DETAILS;
            case "m", "comment" -> AgentCommand.COMMENT;
            case "o", "overdue" -> AgentCommand.OVERDUE_TOGGLE;
            case "r", "refresh" -> AgentCommand.REFRESH;
            case "q", "logout", "quit", "exit" -> AgentCommand.QUIT;
            default -> AgentCommand.UNKNOWN;
        };
    }

    /**
     * Loads agent's assigned tickets via GET /api/tickets/mine.
     */
    private List<TicketListResponse> loadTickets(String authHeader) {
        try {
            return ui.showSpinner("Loading queue...", () -> apiClient.getMyTickets(authHeader));
        } catch (Exception e) {
            ui.printError("Failed to fetch tickets: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Renders the Agent Workspace dashboard with 4 stats and 3 stacked stages.
     */
    private void renderScreen(UserResponse user, List<TicketListResponse> allTickets, boolean overdueOnly) {
        String initials = getInitials(user.name());

        // Header
        System.out.println(Ui.CYAN + "+" + Ui.LINE_DASHES.substring(2) + "+" + Ui.RESET);
        String headerLeft = "AGENT WORKSPACE";
        String headerRight = "[" + initials + "] " + user.name() + " (AGENT)    [Logout (Q)]";
        System.out.println(formatRow(headerLeft, headerRight, Ui.DEFAULT_WIDTH));
        System.out.println(Ui.CYAN + "+" + Ui.LINE_DASHES.substring(2) + "+" + Ui.RESET);

        System.out.println(Ui.BOLD + "MANAGE QUEUE & SLA TARGETS" + Ui.RESET);
        ui.printDivider();

        // 4 Stats
        long assignedCount = countStatus(allTickets, "ASSIGNED");
        long inProgCount = countStatus(allTickets, "IN_PROGRESS");
        long resolvedCount = countStatus(allTickets, "RESOLVED");
        long overdueCount = allTickets.stream().filter(t ->
                t.overdue() && !"RESOLVED".equalsIgnoreCase(t.status()) && !"CLOSED".equalsIgnoreCase(t.status())
        ).count();

        System.out.println(Ui.BOLD + "[ STATS ]" + Ui.RESET);
        System.out.printf("  Assigned Queue: %d  |  In Progress: %d  |  Resolved: %d  |  Overdue SLA: %s%n",
                assignedCount, inProgCount, resolvedCount,
                overdueCount > 0 ? (Ui.BOLD_RED + overdueCount + " [!]" + Ui.RESET) : String.valueOf(overdueCount));
        ui.printDivider();

        // Active Filter Banner
        if (overdueOnly) {
            System.out.println(Ui.BOLD_RED + "Showing: Overdue only (" + overdueCount + ")" + Ui.RESET);
        } else {
            System.out.println(Ui.BOLD + "Showing: All Assigned Tickets (" + allTickets.size() + ")" + Ui.RESET);
        }

        // Apply overdue filter if active
        List<TicketListResponse> visibleTickets = overdueOnly
                ? allTickets.stream().filter(t -> t.overdue() && !"RESOLVED".equalsIgnoreCase(t.status()) && !"CLOSED".equalsIgnoreCase(t.status())).collect(Collectors.toList())
                : allTickets;

        // Render 3 Stacked Sections: ASSIGNED, IN_PROGRESS, RESOLVED
        renderStageSection("ASSIGNED", visibleTickets);
        renderStageSection("IN_PROGRESS", visibleTickets);
        renderStageSection("RESOLVED", visibleTickets);

        printControls();
    }

    private void renderStageSection(String stage, List<TicketListResponse> list) {
        String stageLabel = stage.replace("_", " ");
        List<TicketListResponse> stageTickets = list.stream()
                .filter(t -> stage.equalsIgnoreCase(t.status()))
                .sorted((a, b) -> Integer.compare(b.urgencyScore(), a.urgencyScore())) // Highest urgency first
                .collect(Collectors.toList());

        System.out.println();
        System.out.println(Ui.CYAN + "=== " + Ui.BOLD + stageLabel + " (" + stageTickets.size() + ")" + Ui.RESET + Ui.CYAN + " " + "=".repeat(Math.max(1, Ui.DEFAULT_WIDTH - stageLabel.length() - 10)) + Ui.RESET);

        if (stageTickets.isEmpty()) {
            System.out.println("  " + Ui.DIM + "[ No tickets in this stage ]" + Ui.RESET);
        } else {
            for (TicketListResponse t : stageTickets) {
                renderTicketRow(t);
            }
        }
    }

    private void renderTicketRow(TicketListResponse t) {
        ui.printDivider();
        String overdueTag = ui.overdueTag(t.overdue(), t.status());
        String ticketNum = Ui.BOLD + (t.ticketNumber() != null ? t.ticketNumber() : "HD-" + t.ticketId()) + Ui.RESET;
        String urgency = urgencyLevelBadge(t.urgencyLevel());
        String category = t.categoryName() != null ? ("* " + t.categoryName()) : "* General";

        System.out.printf("  %s%s  %s  %s%n", overdueTag, ticketNum, urgency, category);

        if (t.title() != null && !t.title().isBlank()) {
            System.out.println("    " + Ui.BOLD + ui.truncate(t.title(), 68) + Ui.RESET);
        }

        String priority = t.requestedPriority() != null ? t.requestedPriority() : "LOW";
        System.out.printf("    Requested: %s%n", priority);
    }

    private void printControls() {
        ui.printDoubleDivider();
        System.out.println("  [S]tart Work | [D]one (Resolve) | [T]icket Details | [M] Comment | [O]verdue Only | [R]efresh | [Q]uit");
        ui.printDoubleDivider();
    }

    /**
     * S: Start work on an ASSIGNED ticket (move ASSIGNED -> IN_PROGRESS).
     */
    private boolean handleStartWork(List<TicketListResponse> tickets, String authHeader) {
        String input = ui.readLine("Enter ticket number to start work (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);
        Optional<TicketListResponse> opt = tickets.stream()
                .filter(t -> ticketNumber.equalsIgnoreCase(t.ticketNumber()))
                .findFirst();

        if (opt.isEmpty()) {
            ui.printError("Ticket " + ticketNumber + " not found in your assigned queue.");
            return false;
        }

        TicketListResponse ticket = opt.get();
        if (!"ASSIGNED".equalsIgnoreCase(ticket.status())) {
            ui.printError("Invalid stage: ticket " + ticketNumber + " is currently " + ticket.status() + " (must be ASSIGNED to start work).");
            return false;
        }

        String confirm = ui.readLine("Start work on " + ticketNumber + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        try {
            ui.showSpinner("Starting work on " + ticketNumber + "...", () -> {
                apiClient.updateTicketStatus(ticket.ticketId(), "IN_PROGRESS", authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticketNumber + " is now IN_PROGRESS.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to update ticket status.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to update status: " + e.getMessage());
            return false;
        }
    }

    /**
     * D: Resolve an IN_PROGRESS ticket (move IN_PROGRESS -> RESOLVED).
     */
    private boolean handleResolveTicket(List<TicketListResponse> tickets, String authHeader) {
        String input = ui.readLine("Enter ticket number to resolve (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);
        Optional<TicketListResponse> opt = tickets.stream()
                .filter(t -> ticketNumber.equalsIgnoreCase(t.ticketNumber()))
                .findFirst();

        if (opt.isEmpty()) {
            ui.printError("Ticket " + ticketNumber + " not found in your assigned queue.");
            return false;
        }

        TicketListResponse ticket = opt.get();
        if (!"IN_PROGRESS".equalsIgnoreCase(ticket.status())) {
            ui.printError("Invalid stage: ticket " + ticketNumber + " is currently " + ticket.status() + " (must be IN_PROGRESS to resolve).");
            return false;
        }

        String confirm = ui.readLine("Resolve ticket " + ticketNumber + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        try {
            ui.showSpinner("Resolving ticket " + ticketNumber + "...", () -> {
                apiClient.updateTicketStatus(ticket.ticketId(), "RESOLVED", authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticketNumber + " marked as RESOLVED.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to resolve ticket.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to resolve ticket: " + e.getMessage());
            return false;
        }
    }

    /**
     * T: View Ticket Details modal for agents.
     * Shows full details including urgency analysis factors and comment timeline.
     */
    private boolean handleViewTicket(String authHeader) {
        String input = ui.readLine("Enter ticket number (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);

        try {
            TicketDetailResponse ticket = ui.showSpinner("Loading ticket details...", () ->
                    apiClient.getTicketDetail(ticketNumber, authHeader)
            );

            renderAgentTicketDetailView(ticket);

            ui.readLine("Press Enter to go back...");
            return true;
        } catch (NoSuchElementException e) {
            ui.printError("Ticket not found, or you don't have access to it.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Error loading ticket details.");
            return false;
        }
    }

    /**
     * Normalizes input into HD-XXXX format.
     */
    /**
     * Comment flow ([M] Comment).
     * Delegates to shared CommentFlow.
     */
    private boolean handleComment(String authHeader) {
        return commentFlow.run(authHeader, false);
    }

    private String normalizeTicketNumber(String input) {
        String clean = input.trim().toUpperCase();
        if (clean.startsWith("HD-")) {
            return clean;
        }
        if (clean.startsWith("HD")) {
            return "HD-" + clean.substring(2);
        }
        return "HD-" + clean;
    }

    /**
     * Renders ticket details for agent view (includes urgency analysis factors, no assignment history).
     */
    private void renderAgentTicketDetailView(TicketDetailResponse ticket) {
        System.out.println();
        ui.printBoxHeader("TICKET DETAILS: " + ticket.ticketNumber());
        System.out.printf("  Ticket Number     : %s%n", ticket.ticketNumber());
        System.out.printf("  Title             : %s%n", ticket.title());
        System.out.printf("  Status            : %s%n", ui.statusBadge(ticket.status()));
        System.out.printf("  Category          : %s%n", ticket.categoryName() != null ? ticket.categoryName() : "General");
        System.out.printf("  Creator           : %s%n", ticket.creatorName() != null ? ticket.creatorName() : "Student");
        System.out.printf("  Urgency Rating    : %s%n", ui.urgencyBadge(ticket.urgencyLevel(), ticket.urgencyScore()));
        System.out.printf("  Requested Priority: %s%n", ticket.requestedPriority() != null ? ticket.requestedPriority() : "LOW");

        String dueAtStr = ticket.dueAt() != null ? ui.formatDateTime(ticket.dueAt()) : "None";
        System.out.printf("  SLA Deadline      : %s%n", dueAtStr);

        String overdueTag = ui.overdueTag(ticket.overdue(), ticket.status());
        System.out.printf("  SLA / Overdue     : %s%n", overdueTag.isEmpty() ? "On Schedule" : overdueTag.trim());
        System.out.printf("  Created Date      : %s%n", ui.formatDateTime(ticket.createdAt()));
        System.out.printf("  Last Updated      : %s%n", ui.formatDateTime(ticket.updatedAt()));
        System.out.println();
        System.out.println("  Description:");
        if (ticket.description() != null && !ticket.description().isBlank()) {
            for (String line : ticket.description().split("\n")) {
                System.out.println("    " + line);
            }
        } else {
            System.out.println("    (No description provided)");
        }

        // Urgency Analysis Factors Section (visible to agents)
        ui.printBoxHeader("URGENCY ANALYSIS FACTORS");
        List<String> reasons = ticket.reasons();
        if (reasons == null || reasons.isEmpty()) {
            System.out.println("  No specific urgency factors triggered.");
        } else {
            for (String reason : reasons) {
                System.out.println("  * " + reason);
            }
        }

        // Comments Timeline Section
        List<CommentDetailResponse> comments = ticket.comments();
        int commentCount = comments != null ? comments.size() : 0;
        ui.printBoxHeader("COMMENTS (" + commentCount + ")");
        if (comments == null || comments.isEmpty()) {
            System.out.println("  No comments yet.");
        } else {
            for (int i = 0; i < comments.size(); i++) {
                CommentDetailResponse c = comments.get(i);
                String role = "User";
                if (ticket.assignedAgentName() != null && ticket.assignedAgentName().equalsIgnoreCase(c.authorName())) {
                    role = "Agent";
                } else if (ticket.creatorName() != null && ticket.creatorName().equalsIgnoreCase(c.authorName())) {
                    role = "Student";
                }
                String issueTag = "ISSUE".equalsIgnoreCase(c.commentType()) ? (Ui.BOLD_RED + " [Issue]" + Ui.RESET) : "";
                String time = ui.formatDateTime(c.createdAt());

                System.out.printf("  [%d] %s (%s)%s - %s%n", (i + 1), c.authorName(), role, issueTag, time);
                if (c.commentText() != null) {
                    for (String line : c.commentText().split("\n")) {
                        System.out.println("      " + line);
                    }
                }
                System.out.println();
            }
        }

        ui.printBoxFooter();
    }

    private String urgencyLevelBadge(String level) {
        if (level == null) level = "LOW";
        return switch (level.toUpperCase()) {
            case "URGENT" -> Ui.BOLD_RED + "[URGENT]" + Ui.RESET;
            case "HIGH" -> Ui.LIGHT_RED + "[HIGH]" + Ui.RESET;
            case "MEDIUM" -> Ui.YELLOW + "[MEDIUM]" + Ui.RESET;
            default -> Ui.DIM + "[LOW]" + Ui.RESET;
        };
    }

    private long countStatus(List<TicketListResponse> list, String status) {
        return list.stream().filter(t -> status.equalsIgnoreCase(t.status())).count();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "AG";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String formatRow(String left, String right, int width) {
        int spacing = width - left.length() - right.length() - 4;
        if (spacing < 1) spacing = 1;
        return "| " + left + " ".repeat(spacing) + right + " |";
    }
}
