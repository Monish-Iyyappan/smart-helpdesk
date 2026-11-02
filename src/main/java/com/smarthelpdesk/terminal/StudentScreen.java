package com.smarthelpdesk.terminal;

import com.smarthelpdesk.dto.CategoryResponse;
import com.smarthelpdesk.dto.CommentDetailResponse;
import com.smarthelpdesk.dto.TicketDetailResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Screen 2: Student Portal.
 * Handles ticket dashboard, status filtering, ticket details, creating, joining, and commenting on tickets.
 */
@Component
public class StudentScreen {

    public enum StudentCommand {
        CREATE, JOIN, TICKET, COMMENT, FILTER, REFRESH, QUIT, UNKNOWN
    }

    private final Ui ui;
    private final ApiClient apiClient;
    private final CommentFlow commentFlow;

    public StudentScreen(Ui ui, ApiClient apiClient) {
        this.ui = ui;
        this.apiClient = apiClient;
        this.commentFlow = new CommentFlow(apiClient, ui);
    }

    public void show(TerminalSession session) {
        if (!"STUDENT".equalsIgnoreCase(session.getRole())) {
            ui.printError("Access denied: Student screen requires STUDENT role. Current role: " + session.getRole());
            return;
        }

        List<TicketListResponse> allTickets = loadTickets(session.authHeader());
        String currentFilter = "ALL";
        boolean redrawScreen = true;

        while (true) {
            if (redrawScreen) {
                renderScreen(session.user(), allTickets, currentFilter);
            }
            redrawScreen = false;

            String input = ui.readLine("Enter your choice (C / J / T / M / F / R / Q): ").trim();
            StudentCommand cmd = parseCommand(input);

            switch (cmd) {
                case CREATE -> {
                    boolean created = handleCreateTicket(session.authHeader());
                    if (created) {
                        allTickets = loadTickets(session.authHeader());
                        redrawScreen = true;
                    } else {
                        printControls();
                    }
                }
                case JOIN -> {
                    boolean joined = handleJoinTicket(session.authHeader());
                    if (joined) {
                        allTickets = loadTickets(session.authHeader());
                        redrawScreen = true;
                    } else {
                        printControls();
                    }
                }
                case TICKET -> {
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
                case FILTER -> {
                    String newFilter = handleFilterSelection(allTickets, currentFilter);
                    if (!newFilter.equals(currentFilter)) {
                        currentFilter = newFilter;
                    }
                    redrawScreen = true;
                }
                case REFRESH -> {
                    allTickets = loadTickets(session.authHeader());
                    ui.printSuccess("Tickets refreshed.");
                    redrawScreen = true;
                }
                case QUIT -> {
                    System.out.println("Logging out...");
                    return;
                }
                case UNKNOWN -> {
                    ui.printError("Invalid choice. Type create, join, ticket, comment, filter, refresh, or quit.");
                    printControls();
                }
            }
        }
    }

    /**
     * Maps user input to a StudentCommand. All aliases defined in one place.
     */
    private StudentCommand parseCommand(String input) {
        if (input == null) return StudentCommand.UNKNOWN;
        String clean = input.trim().toLowerCase();
        return switch (clean) {
            case "c", "create", "new" -> StudentCommand.CREATE;
            case "j", "join" -> StudentCommand.JOIN;
            case "t", "ticket", "details", "view" -> StudentCommand.TICKET;
            case "m", "comment" -> StudentCommand.COMMENT;
            case "f", "filter" -> StudentCommand.FILTER;
            case "r", "refresh" -> StudentCommand.REFRESH;
            case "q", "logout", "quit", "exit" -> StudentCommand.QUIT;
            default -> StudentCommand.UNKNOWN;
        };
    }

    /**
     * Loads tickets from the REST API using an animated spinner.
     */
    private List<TicketListResponse> loadTickets(String authHeader) {
        try {
            return ui.showSpinner("Loading tickets...", () -> apiClient.getMyTickets(authHeader));
        } catch (Exception e) {
            ui.printError("Failed to fetch tickets: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Renders the Student portal dashboard.
     */
    private void renderScreen(UserResponse user, List<TicketListResponse> allTickets, String filter) {
        String initials = getInitials(user.name());

        // Header
        System.out.println(Ui.CYAN + "+" + Ui.LINE_DASHES.substring(2) + "+" + Ui.RESET);
        String headerLeft = "MY TICKETS";
        String headerRight = "[" + initials + "] " + user.name() + " (STUDENT)    [Logout (Q)]";
        System.out.println(formatRow(headerLeft, headerRight, Ui.DEFAULT_WIDTH));
        System.out.println(Ui.CYAN + "+" + Ui.LINE_DASHES.substring(2) + "+" + Ui.RESET);

        // Welcome banner
        System.out.println(Ui.BOLD + "WELCOME BACK, " + user.name().toUpperCase() + Ui.RESET);
        System.out.println("Track submitted requests, join shared tickets, and check live updates.");
        ui.printDivider();

        // 4 Stats Calculation
        int total = allTickets.size();
        long activeCount = allTickets.stream().filter(t -> {
            String s = t.status();
            return "OPEN".equalsIgnoreCase(s) || "ASSIGNED".equalsIgnoreCase(s) || "IN_PROGRESS".equalsIgnoreCase(s);
        }).count();
        long resolvedCount = allTickets.stream().filter(t -> {
            String s = t.status();
            return "RESOLVED".equalsIgnoreCase(s) || "CLOSED".equalsIgnoreCase(s);
        }).count();
        long overdueCount = allTickets.stream().filter(t ->
                t.overdue() && !"RESOLVED".equalsIgnoreCase(t.status()) && !"CLOSED".equalsIgnoreCase(t.status())
        ).count();

        System.out.println(Ui.BOLD + "[ STATS ]" + Ui.RESET);
        System.out.printf("  Total Tickets: %d  |  Active In Flight: %d  |  Resolved: %d  |  Overdue SLA: %s%n",
                total, activeCount, resolvedCount,
                overdueCount > 0 ? (Ui.BOLD_RED + overdueCount + " [!]" + Ui.RESET) : String.valueOf(overdueCount));
        ui.printDivider();

        // Filtered tickets
        List<TicketListResponse> filtered = allTickets.stream()
                .filter(t -> "ALL".equals(filter) || filter.equalsIgnoreCase(t.status()))
                .collect(Collectors.toList());

        // Active filter headline
        String filterLabel = getFilterLabel(filter);
        System.out.println(Ui.BOLD + "Showing: " + filterLabel + " (" + filtered.size() + ")" + Ui.RESET);
        ui.printDivider();

        // Ticket list
        if (allTickets.isEmpty()) {
            System.out.println();
            System.out.println(ui.center("No Tickets Found. Create your first ticket to get started.", Ui.DEFAULT_WIDTH));
            System.out.println();
        } else if (filtered.isEmpty()) {
            System.out.println();
            System.out.println(ui.center("No tickets match your filter. Select [F] to change filter.", Ui.DEFAULT_WIDTH));
            System.out.println();
        } else {
            for (TicketListResponse t : filtered) {
                renderTicketRow(t);
            }
        }

        printControls();
    }

    private void printControls() {
        ui.printDoubleDivider();
        System.out.println("  [C]reate | [J]oin | [T]icket details | [M] Comment | [F]ilter | [R]efresh | [Q]uit");
        System.out.println(Ui.DIM + "  Filter = show only tickets with one status." + Ui.RESET);
        ui.printDoubleDivider();
    }

    private void renderTicketRow(TicketListResponse t) {
        ui.printDivider();
        String overdueTag = ui.overdueTag(t.overdue(), t.status());
        String ticketNum = Ui.BOLD + (t.ticketNumber() != null ? t.ticketNumber() : "HD-" + t.ticketId()) + Ui.RESET;
        String category = t.categoryName() != null ? ("* " + t.categoryName()) : "* General";
        String status = ui.statusBadge(t.status());
        String urgency = ui.urgencyBadge(t.urgencyLevel(), t.urgencyScore());

        System.out.printf("%s%s  %s    %s  %s%n", overdueTag, ticketNum, category, status, urgency);

        if (t.title() != null && !t.title().isBlank()) {
            System.out.println("    " + Ui.BOLD + ui.truncate(t.title(), 70) + Ui.RESET);
        }
        if (t.description() != null && !t.description().isBlank()) {
            System.out.println("    " + Ui.DIM + ui.truncate(t.description().replace("\n", " "), 75) + Ui.RESET);
        }

        String priority = t.requestedPriority() != null ? t.requestedPriority() : "LOW";
        String agent = t.assignedTo() != null ? "Agent #" + t.assignedTo() : "Unassigned";
        String created = ui.formatDate(t.createdAt());
        System.out.printf("    Requested: %s | Agent: %s | Created: %s%n", priority, agent, created);
    }

    /**
     * Status filter picker. Accepts 1-6 or status names.
     */
    private String handleFilterSelection(List<TicketListResponse> allTickets, String currentFilter) {
        int total = allTickets.size();
        long openC = countStatus(allTickets, "OPEN");
        long assignedC = countStatus(allTickets, "ASSIGNED");
        long inProgC = countStatus(allTickets, "IN_PROGRESS");
        long resolvedC = countStatus(allTickets, "RESOLVED");
        long closedC = countStatus(allTickets, "CLOSED");

        System.out.println();
        System.out.println(Ui.BOLD + "Filter by Status:" + Ui.RESET);
        System.out.println("  1 All (" + total + ")");
        System.out.println("  2 Open (" + openC + ")");
        System.out.println("  3 Assigned (" + assignedC + ")");
        System.out.println("  4 In Progress (" + inProgC + ")");
        System.out.println("  5 Resolved (" + resolvedC + ")");
        System.out.println("  6 Closed (" + closedC + ")");
        System.out.println();

        String input = ui.readLine("Pick a number (1-6) or status name (b to cancel): ").trim().toLowerCase();
        if (input.equals("b") || input.isEmpty()) {
            return currentFilter;
        }

        return switch (input) {
            case "1", "all" -> "ALL";
            case "2", "open" -> "OPEN";
            case "3", "assigned" -> "ASSIGNED";
            case "4", "in progress", "in_progress", "inprogress" -> "IN_PROGRESS";
            case "5", "resolved" -> "RESOLVED";
            case "6", "closed" -> "CLOSED";
            default -> {
                ui.printError("Invalid filter choice. Use 1-6 or status name.");
                yield currentFilter;
            }
        };
    }

    /**
     * Create Ticket flow.
     */
    private boolean handleCreateTicket(String authHeader) {
        List<CategoryResponse> categories;
        try {
            categories = apiClient.getCategories(authHeader);
        } catch (Exception e) {
            ui.printError("Cannot reach the server. Is the backend running?");
            return false;
        }

        if (categories == null || categories.isEmpty()) {
            ui.printError("No ticket categories found on the server.");
            return false;
        }

        System.out.println();
        ui.printSubHeader("CREATE NEW TICKET");

        // 1. Category
        System.out.println(Ui.BOLD + "Choose a Category:" + Ui.RESET);
        for (int i = 0; i < categories.size(); i++) {
            System.out.printf("  %d %s%n", (i + 1), categories.get(i).categoryName());
        }

        Integer selectedCategoryId = null;
        String selectedCategoryName = null;
        while (selectedCategoryId == null) {
            String prompt = String.format("Pick a category (1-%d, b to cancel): ", categories.size());
            String input = ui.readLine(prompt).trim();
            if (input.equalsIgnoreCase("b")) {
                return false;
            }
            try {
                int idx = Integer.parseInt(input);
                if (idx >= 1 && idx <= categories.size()) {
                    CategoryResponse cat = categories.get(idx - 1);
                    selectedCategoryId = cat.categoryId();
                    selectedCategoryName = cat.categoryName();
                } else {
                    ui.printError("Please pick a number between 1 and " + categories.size() + ".");
                }
            } catch (NumberFormatException e) {
                ui.printError("Please pick a number between 1 and " + categories.size() + ".");
            }
        }

        // 2. Title (5-100 characters)
        String title = null;
        while (title == null) {
            String input = ui.readLine("Title (5-100 chars, b to cancel): ").trim();
            if (input.equalsIgnoreCase("b")) {
                return false;
            }
            if (input.length() >= 5 && input.length() <= 100) {
                title = input;
            } else {
                ui.printError("Title must be 5-100 characters.");
            }
        }

        // 3. Description (10-1000 characters)
        String description = null;
        while (description == null) {
            String input = ui.readLine("Description (10-1000 chars, b to cancel): ").trim();
            if (input.equalsIgnoreCase("b")) {
                return false;
            }
            if (input.length() >= 10 && input.length() <= 1000) {
                description = input;
            } else {
                ui.printError("Description must be 10-1000 characters.");
            }
        }

        // 4. Priority (1-4, Enter alone = MEDIUM)
        System.out.println(Ui.BOLD + "Priority:" + Ui.RESET);
        System.out.println("  1 LOW");
        System.out.println("  2 MEDIUM (Default)");
        System.out.println("  3 HIGH");
        System.out.println("  4 URGENT");

        String priority = null;
        while (priority == null) {
            String input = ui.readLine("Pick priority (1-4) [Enter for MEDIUM, b to cancel]: ").trim();
            if (input.equalsIgnoreCase("b")) {
                return false;
            }
            if (input.isEmpty() || input.equals("2") || input.equalsIgnoreCase("medium")) {
                priority = "MEDIUM";
            } else if (input.equals("1") || input.equalsIgnoreCase("low")) {
                priority = "LOW";
            } else if (input.equals("3") || input.equalsIgnoreCase("high")) {
                priority = "HIGH";
            } else if (input.equals("4") || input.equalsIgnoreCase("urgent")) {
                priority = "URGENT";
            } else {
                ui.printError("Please enter 1-4, or press Enter for MEDIUM.");
            }
        }

        // 5. Summary & Confirmation
        System.out.println();
        ui.printSubHeader("TICKET SUMMARY");
        System.out.println("  Category   : " + selectedCategoryName);
        System.out.println("  Title      : " + title);
        System.out.println("  Description: " + description);
        System.out.println("  Priority   : " + priority);
        ui.printDivider();

        String confirm = ui.readLine("Submit? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        // 6. Submit via REST API
        final Integer catId = selectedCategoryId;
        final String tTitle = title;
        final String tDesc = description;
        final String tPrio = priority;

        try {
            String ticketNumber = ui.showSpinner("Submitting ticket...", () ->
                    apiClient.createTicket(catId, tTitle, tDesc, tPrio, authHeader)
            );
            ui.printSuccess("Ticket " + ticketNumber + " created.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to create ticket.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to create ticket.");
            return false;
        }
    }

    /**
     * Join Ticket flow.
     */
    private boolean handleJoinTicket(String authHeader) {
        String input = ui.readLine("Enter ticket number to join (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);

        try {
            boolean joined = ui.showSpinner("Joining ticket...", () ->
                    apiClient.joinTicket(ticketNumber, authHeader)
            );
            if (joined) {
                ui.printSuccess("Joined ticket " + ticketNumber + ".");
                return true;
            } else {
                ui.printError("Could not join this ticket: already joined.");
                return false;
            }
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Could not join this ticket.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Could not join this ticket.");
            return false;
        }
    }

    /**
     * View Ticket Details modal. Returns true if successfully viewed, false on cancel/error.
     */
    private boolean handleViewTicket(String authHeader) {
        String input = ui.readLine("Enter ticket number (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);

        try {
            // Fetch ticket details with animated spinner
            TicketDetailResponse ticket = ui.showSpinner("Loading ticket details...", () ->
                    apiClient.getTicketDetail(ticketNumber, authHeader)
            );

            // Render details in boxed sections (no urgency breakdown or assignment history for students)
            renderTicketDetailView(ticket);

            // Keep "Press Enter to go back" ONLY after successful ticket details view
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
     * Comment flow ([M] Comment).
     * Delegates to shared CommentFlow.
     */
    private boolean handleComment(String authHeader) {
        return commentFlow.run(authHeader, true);
    }

    /**
     * Normalizes input into HD-XXXX format.
     */
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
     * Renders ticket details in distinct boxed sections for student view.
     */
    private void renderTicketDetailView(TicketDetailResponse ticket) {
        System.out.println();
        ui.printBoxHeader("TICKET DETAILS: " + ticket.ticketNumber());
        System.out.printf("  Ticket Number     : %s%n", ticket.ticketNumber());
        System.out.printf("  Title             : %s%n", ticket.title());
        System.out.printf("  Status            : %s%n", ui.statusBadge(ticket.status()));
        System.out.printf("  Category          : %s%n", ticket.categoryName() != null ? ticket.categoryName() : "General");
        System.out.printf("  Requested Priority: %s%n", ticket.requestedPriority() != null ? ticket.requestedPriority() : "LOW");

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

        // Assigned Agent Section
        ui.printBoxHeader("ASSIGNED AGENT");
        String agentName = ticket.assignedAgentName();
        if (agentName == null && ticket.assignedTo() != null) {
            agentName = "Agent #" + ticket.assignedTo();
        }
        System.out.println("  " + (agentName != null ? agentName : "Not assigned yet"));

        // Comments Section
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

    private String getFilterLabel(String filter) {
        return switch (filter.toUpperCase()) {
            case "OPEN" -> "Open";
            case "ASSIGNED" -> "Assigned";
            case "IN_PROGRESS" -> "In Progress";
            case "RESOLVED" -> "Resolved";
            case "CLOSED" -> "Closed";
            default -> "All";
        };
    }

    private long countStatus(List<TicketListResponse> list, String status) {
        return list.stream().filter(t -> status.equalsIgnoreCase(t.status())).count();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "ST";
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
