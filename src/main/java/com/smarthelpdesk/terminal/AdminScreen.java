package com.smarthelpdesk.terminal;

import com.smarthelpdesk.assignment.AssignmentRecord;
import com.smarthelpdesk.dto.AdminStatsResponse;
import com.smarthelpdesk.dto.ClosedTicketResponse;
import com.smarthelpdesk.dto.CommentDetailResponse;
import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.dto.IncidentTicketResponse;
import com.smarthelpdesk.dto.TicketDetailResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Screen 4: Admin Center.
 * Handles full administration: system stats, ticket queues, ticket details,
 * comments, status advancement (Start/Done), agent reassignment, closing, reopening,
 * incident management & ticket linking, agent provisioning, and audit logs.
 */
@Component
public class AdminScreen extends BaseScreen {

    public enum AdminTab {
        STATS(1, "Stats"),
        ALL_TICKETS(2, "All Tickets"),
        UNASSIGNED(3, "Unassigned"),
        OVERDUE(4, "Overdue SLA"),
        INCIDENTS(5, "Incidents"),
        AGENTS(6, "Agents"),
        CLOSED_LOG(7, "Closed Log");

        public final int number;
        public final String label;

        AdminTab(int number, String label) {
            this.number = number;
            this.label = label;
        }
    }

    public enum AdminCommand {
        TAB_STATS, TAB_ALL, TAB_UNASSIGNED, TAB_OVERDUE, TAB_INCIDENTS, TAB_AGENTS, TAB_CLOSED_LOG,
        TICKET_DETAILS, COMMENT, INCIDENT_TICKETS, CLOSE_TICKET, REOPEN_TICKET,
        DECLARE_INCIDENT, ADD_AGENT, LINK_TICKET, REFRESH, QUIT, UNKNOWN
    }

    private final Ui ui;
    private final ApiClient apiClient;
    private final CommentFlow commentFlow;

    public AdminScreen(Ui ui, ApiClient apiClient) {
        super("Enter your choice (1-7 / T / M / I / C / O / N / A / L / R / Q): ");
        this.ui = ui;
        this.apiClient = apiClient;
        this.commentFlow = new CommentFlow(apiClient, ui);
    }

    public void show(TerminalSession session) {
        if (!"ADMIN".equalsIgnoreCase(session.getRole())) {
            ui.printError("Access denied: Admin Center requires ADMIN role. Current role: " + session.getRole());
            return;
        }

        AdminTab currentTab = AdminTab.STATS;
        AdminDataHolder data = loadAllData(session.authHeader());
        boolean redrawScreen = true;

        while (true) {
            if (redrawScreen) {
                renderScreen(session.user(), currentTab, data);
            }
            redrawScreen = false;

            showControlsAndPrompt();
            String input = ui.readLine("").trim();
            AdminCommand cmd = parseCommand(input);

            switch (cmd) {
                case TAB_STATS -> {
                    currentTab = AdminTab.STATS;
                    redrawScreen = true;
                }
                case TAB_ALL -> {
                    currentTab = AdminTab.ALL_TICKETS;
                    redrawScreen = true;
                }
                case TAB_UNASSIGNED -> {
                    currentTab = AdminTab.UNASSIGNED;
                    redrawScreen = true;
                }
                case TAB_OVERDUE -> {
                    currentTab = AdminTab.OVERDUE;
                    redrawScreen = true;
                }
                case TAB_INCIDENTS -> {
                    currentTab = AdminTab.INCIDENTS;
                    redrawScreen = true;
                }
                case TAB_AGENTS -> {
                    currentTab = AdminTab.AGENTS;
                    redrawScreen = true;
                }
                case TAB_CLOSED_LOG -> {
                    currentTab = AdminTab.CLOSED_LOG;
                    redrawScreen = true;
                }
                case TICKET_DETAILS -> {
                    boolean success = handleViewTicket(session.authHeader());
                    if (success) {
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case COMMENT -> {
                    boolean updated = handleComment(session.authHeader());
                    if (updated) {
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case INCIDENT_TICKETS -> {
                    handleIncidentTickets(session.authHeader());
                }
                case CLOSE_TICKET -> {
                    boolean success = handleCloseTicket(session.authHeader());
                    if (success) {
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case REOPEN_TICKET -> {
                    boolean success = handleReopenTicket(session.authHeader());
                    if (success) {
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case DECLARE_INCIDENT -> {
                    boolean created = handleDeclareIncident(session.authHeader());
                    if (created) {
                        currentTab = AdminTab.INCIDENTS;
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case ADD_AGENT -> {
                    boolean created = handleAddAgent(session.authHeader());
                    if (created) {
                        currentTab = AdminTab.AGENTS;
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case LINK_TICKET -> {
                    boolean linked = handleLinkTicket(data, session.authHeader());
                    if (linked) {
                        data = loadAllData(session.authHeader());
                        redrawScreen = true;
                    }
                }
                case REFRESH -> {
                    data = loadAllData(session.authHeader());
                    ui.printSuccess("Refreshed dashboard.");
                    redrawScreen = true;
                }
                case QUIT -> {
                    System.out.println("Logging out from Admin Center...");
                    return;
                }
                case UNKNOWN -> {
                    ui.printError("Invalid choice. Enter 1-7 for tabs, or T, M, I, C, O, N, A, L, R, Q.");
                }
            }
        }
    }

    /**
     * Maps user input to an AdminCommand (letters or words, any case, trimmed).
     */
    private AdminCommand parseCommand(String input) {
        if (input == null) return AdminCommand.UNKNOWN;
        String clean = input.trim().toLowerCase();
        return switch (clean) {
            case "1", "stats", "overview" -> AdminCommand.TAB_STATS;
            case "2", "all", "tickets", "all tickets" -> AdminCommand.TAB_ALL;
            case "3", "unassigned" -> AdminCommand.TAB_UNASSIGNED;
            case "4", "overdue", "sla" -> AdminCommand.TAB_OVERDUE;
            case "5", "tab 5", "tab incidents" -> AdminCommand.TAB_INCIDENTS;
            case "6", "agents", "tab agents" -> AdminCommand.TAB_AGENTS;
            case "7", "closed", "log", "closed log" -> AdminCommand.TAB_CLOSED_LOG;
            case "t", "ticket", "details", "view" -> AdminCommand.TICKET_DETAILS;
            case "m", "comment" -> AdminCommand.COMMENT;
            case "i", "incident tickets", "incidents", "incident ticket" -> AdminCommand.INCIDENT_TICKETS;
            case "c", "close", "close ticket" -> AdminCommand.CLOSE_TICKET;
            case "o", "reopen", "reopen ticket" -> AdminCommand.REOPEN_TICKET;
            case "n", "declare", "incident", "declare incident", "new incident" -> AdminCommand.DECLARE_INCIDENT;
            case "a", "agent", "add agent" -> AdminCommand.ADD_AGENT;
            case "l", "link", "link ticket" -> AdminCommand.LINK_TICKET;
            case "r", "refresh" -> AdminCommand.REFRESH;
            case "q", "logout", "quit", "exit" -> AdminCommand.QUIT;
            default -> AdminCommand.UNKNOWN;
        };
    }

    /**
     * Holds snapshot of admin data across tabs.
     */
    private static class AdminDataHolder {
        AdminStatsResponse stats;
        List<TicketListResponse> allTickets = new ArrayList<>();
        List<TicketListResponse> unassignedTickets = new ArrayList<>();
        List<TicketListResponse> overdueTickets = new ArrayList<>();
        List<IncidentResponse> incidents = new ArrayList<>();
        List<UserResponse> agents = new ArrayList<>();
        List<ClosedTicketResponse> closedLog = new ArrayList<>();
    }

    private AdminDataHolder loadAllData(String authHeader) {
        AdminDataHolder holder = new AdminDataHolder();
        try {
            ui.showSpinner("Loading admin data...", () -> {
                try {
                    holder.stats = apiClient.getAdminStats(authHeader);
                } catch (Exception ignored) {}
                try {
                    holder.allTickets = apiClient.getAllAdminTickets(authHeader);
                } catch (Exception ignored) {}
                try {
                    holder.unassignedTickets = apiClient.getUnassignedTickets(authHeader);
                } catch (Exception ignored) {}
                try {
                    holder.overdueTickets = apiClient.getOverdueTickets(authHeader);
                } catch (Exception ignored) {}
                try {
                    holder.incidents = apiClient.getIncidents(authHeader);
                } catch (Exception ignored) {}
                try {
                    holder.agents = apiClient.getAgents(authHeader);
                } catch (Exception ignored) {}
                try {
                    holder.closedLog = apiClient.getClosedTicketsLog(authHeader);
                } catch (Exception ignored) {}
                return null;
            });
        } catch (Exception e) {
            ui.printError("Error loading data: " + e.getMessage());
        }
        return holder;
    }

    /**
     * Renders screen header, tab bar, active tab content, and controls.
     */
    private void renderScreen(UserResponse user, AdminTab currentTab, AdminDataHolder data) {
        String initials = getInitials(user.name());

        // Header
        System.out.println(Ui.CYAN + "+" + Ui.LINE_DASHES.substring(2) + "+" + Ui.RESET);
        String headerLeft = "ADMIN CENTER";
        String headerRight = "[" + initials + "] " + user.name() + " (ADMIN)    [Logout (Q)]";
        System.out.println(formatRow(headerLeft, headerRight, Ui.DEFAULT_WIDTH));
        System.out.println(Ui.CYAN + "+" + Ui.LINE_DASHES.substring(2) + "+" + Ui.RESET);

        // Tab Navigation Line
        renderTabBar(currentTab, data);
        ui.printDivider();

        // Active Tab Banner
        System.out.println(Ui.BOLD + "Showing Tab: [" + currentTab.number + "] " + currentTab.label + Ui.RESET);
        ui.printDivider();

        // Tab Content
        switch (currentTab) {
            case STATS -> renderStatsTab(data);
            case ALL_TICKETS -> renderTicketListTab("All Tickets", data.allTickets, data.agents, "No tickets registered yet.");
            case UNASSIGNED -> renderTicketListTab("Unassigned Tickets", data.unassignedTickets, data.agents, "No unassigned tickets pending triage.");
            case OVERDUE -> renderTicketListTab("Overdue Tickets", data.overdueTickets, data.agents, "No tickets currently breaching SLA.");
            case INCIDENTS -> renderIncidentsTab(data.incidents);
            case AGENTS -> renderAgentsTab(data.agents, data.allTickets);
            case CLOSED_LOG -> renderClosedLogTab(data.closedLog);
        }
    }

    private void renderTabBar(AdminTab active, AdminDataHolder d) {
        StringBuilder sb1 = new StringBuilder("  TABS: ");
        sb1.append(tabPill(AdminTab.STATS, active, 0)).append(" | ")
           .append(tabPill(AdminTab.ALL_TICKETS, active, d.allTickets.size())).append(" | ")
           .append(tabPill(AdminTab.UNASSIGNED, active, d.unassignedTickets.size())).append(" | ")
           .append(tabPill(AdminTab.OVERDUE, active, d.overdueTickets.size()));

        StringBuilder sb2 = new StringBuilder("        ");
        sb2.append(tabPill(AdminTab.INCIDENTS, active, d.incidents.size())).append(" | ")
           .append(tabPill(AdminTab.AGENTS, active, d.agents.size())).append(" | ")
           .append(tabPill(AdminTab.CLOSED_LOG, active, d.closedLog.size()));

        System.out.println(sb1);
        System.out.println(sb2);
    }

    private String tabPill(AdminTab tab, AdminTab active, int count) {
        String label = (count > 0 && tab != AdminTab.STATS) ? (tab.label + " (" + count + ")") : tab.label;
        if (tab == active) {
            return Ui.BOLD + Ui.GREEN + "<[" + tab.number + "] " + label + ">" + Ui.RESET;
        } else {
            return "[" + tab.number + "] " + label;
        }
    }

    /**
     * Tab 1: Stats tab with boxed sections, aligned numbers, and simple ASCII bars.
     */
    private void renderStatsTab(AdminDataHolder data) {
        AdminStatsResponse s = data.stats;
        int totalTickets = s != null ? s.totalTickets() : data.allTickets.size();
        int unassigned = data.unassignedTickets.size();
        int overdue = s != null ? s.overdueTickets() : data.overdueTickets.size();
        int incidents = s != null ? s.totalIncidents() : data.incidents.size();

        ui.printBoxHeader("SYSTEM OVERVIEW & METRICS");
        System.out.printf("  Total Tickets    : %-4d   |   Unassigned Queue: %-4d%n", totalTickets, unassigned);
        String overdueStr = overdue > 0 ? (Ui.BOLD_RED + overdue + " [!]" + Ui.RESET) : String.valueOf(overdue);
        System.out.printf("  Overdue SLA      : %-13s |   Active Incidents: %-4d%n", overdueStr, incidents);

        // Urgency Breakdown
        ui.printBoxHeader("URGENCY BREAKDOWN");
        Map<String, Integer> urgencyMap = s != null && s.countsByUrgencyLevel() != null ? s.countsByUrgencyLevel() : Map.of();
        int urgentCount = urgencyMap.getOrDefault("URGENT", 0);
        int highCount = urgencyMap.getOrDefault("HIGH", 0);
        int medCount = urgencyMap.getOrDefault("MEDIUM", 0);
        int lowCount = urgencyMap.getOrDefault("LOW", 0);
        int maxUrgency = Math.max(1, Math.max(Math.max(urgentCount, highCount), Math.max(medCount, lowCount)));

        System.out.printf("  %-8s: %3d  %s%n", Ui.BOLD_RED + "URGENT" + Ui.RESET, urgentCount, renderBar(urgentCount, maxUrgency, 20));
        System.out.printf("  %-8s: %3d  %s%n", Ui.LIGHT_RED + "HIGH" + Ui.RESET, highCount, renderBar(highCount, maxUrgency, 20));
        System.out.printf("  %-8s: %3d  %s%n", Ui.YELLOW + "MEDIUM" + Ui.RESET, medCount, renderBar(medCount, maxUrgency, 20));
        System.out.printf("  %-8s: %3d  %s%n", Ui.DIM + "LOW" + Ui.RESET, lowCount, renderBar(lowCount, maxUrgency, 20));

        // Status Distribution
        ui.printBoxHeader("STATUS DISTRIBUTION");
        Map<String, Integer> statusMap = s != null && s.countsByStatus() != null ? s.countsByStatus() : Map.of();
        int maxStatus = statusMap.values().stream().mapToInt(v -> v != null ? v : 0).max().orElse(1);
        if (maxStatus < 1) maxStatus = 1;

        for (String st : List.of("OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED")) {
            int cnt = statusMap.getOrDefault(st, 0);
            System.out.printf("  %-12s: %3d  %s%n", st, cnt, renderBar(cnt, maxStatus, 20));
        }

        // Users By Role
        ui.printBoxHeader("USERS BY ROLE");
        Map<String, Integer> usersMap = s != null && s.usersByRole() != null ? s.usersByRole() : Map.of();
        int maxUsers = usersMap.values().stream().mapToInt(v -> v != null ? v : 0).max().orElse(1);
        if (maxUsers < 1) maxUsers = 1;

        for (String role : List.of("STUDENT", "AGENT", "ADMIN")) {
            int cnt = usersMap.getOrDefault(role, 0);
            System.out.printf("  %-12s: %3d  %s%n", role, cnt, renderBar(cnt, maxUsers, 20));
        }

        ui.printBoxFooter();
    }

    /**
     * Tabs 2, 3, 4: Ticket list tab rendering.
     */
    private void renderTicketListTab(String tabTitle, List<TicketListResponse> tickets, List<UserResponse> agents, String emptyMsg) {
        if (tickets == null || tickets.isEmpty()) {
            System.out.println("  " + Ui.DIM + "[ " + emptyMsg + " ]" + Ui.RESET);
            return;
        }

        for (TicketListResponse t : tickets) {
            renderTicketRow(t, agents);
        }
    }

    private void renderTicketRow(TicketListResponse t, List<UserResponse> agents) {
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

        String priority = t.requestedPriority() != null ? t.requestedPriority() : "LOW";
        String agent = resolveAgentName(t.assignedTo(), agents);
        String created = ui.formatDate(t.createdAt());
        System.out.printf("    Requested: %s | Agent: %s | Created: %s%n", priority, agent, created);
    }

    private String resolveAgentName(Integer assignedTo, List<UserResponse> agents) {
        if (assignedTo == null) return "Unassigned";
        if (agents != null) {
            for (UserResponse u : agents) {
                if (assignedTo.equals(u.userId())) return u.name();
            }
        }
        return "Agent #" + assignedTo;
    }

    /**
     * Tab 5: Campus Incidents list.
     */
    private void renderIncidentsTab(List<IncidentResponse> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            System.out.println("  " + Ui.DIM + "[ No incidents recorded. ]" + Ui.RESET);
            return;
        }

        for (IncidentResponse inc : incidents) {
            ui.printDivider();
            String sevBadge = switch (inc.severity() != null ? inc.severity().toUpperCase() : "LOW") {
                case "CRITICAL" -> Ui.BOLD_RED + "[CRITICAL]" + Ui.RESET;
                case "HIGH" -> Ui.LIGHT_RED + "[HIGH]" + Ui.RESET;
                case "MEDIUM" -> Ui.YELLOW + "[MEDIUM]" + Ui.RESET;
                default -> Ui.DIM + "[LOW]" + Ui.RESET;
            };

            System.out.printf("  %s %s - %s  (Tickets: %d | Open: %d)%n",
                    Ui.BOLD + "INC-" + inc.incidentId() + Ui.RESET, sevBadge, Ui.BOLD + inc.title() + Ui.RESET,
                    inc.ticketCount(), inc.openTicketCount());
            System.out.printf("    Incident ID: %s (Type 'I' then %d to view linked tickets)%n",
                    Ui.CYAN + "INC-" + inc.incidentId() + Ui.RESET, inc.incidentId());
            System.out.printf("    Location   : %s%n", inc.location() != null ? inc.location() : "Campus Wide");
            if (inc.description() != null && !inc.description().isBlank()) {
                System.out.printf("    Description: %s%n", ui.truncate(inc.description(), 68));
            }
        }
    }

    /**
     * Tab 6: Agents directory with active workload.
     */
    private void renderAgentsTab(List<UserResponse> agents, List<TicketListResponse> allTickets) {
        if (agents == null || agents.isEmpty()) {
            System.out.println("  " + Ui.DIM + "[ No agents found. ]" + Ui.RESET);
            return;
        }

        for (UserResponse ag : agents) {
            ui.printDivider();
            long workload = allTickets.stream()
                    .filter(t -> t.assignedTo() != null && t.assignedTo().equals(ag.userId()) && !"RESOLVED".equalsIgnoreCase(t.status()) && !"CLOSED".equalsIgnoreCase(t.status()))
                    .count();

            System.out.printf("  %s (%s)%n", Ui.BOLD + ag.name() + Ui.RESET, ag.email());
            System.out.printf("    Role: %s | Active Workload: %d ticket(s)%n",
                    Ui.CYAN + ag.role() + Ui.RESET, workload);
        }
    }

    /**
     * Tab 7: Closed Tickets audit log.
     */
    private void renderClosedLogTab(List<ClosedTicketResponse> closedLog) {
        if (closedLog == null || closedLog.isEmpty()) {
            System.out.println("  " + Ui.DIM + "[ No closed tickets found. ]" + Ui.RESET);
            return;
        }

        for (ClosedTicketResponse log : closedLog) {
            ui.printDivider();
            String reassignTag = log.reassignmentCount() > 0
                    ? (Ui.YELLOW + " [" + log.reassignmentCount() + " Reassignment" + (log.reassignmentCount() > 1 ? "s" : "") + "]" + Ui.RESET)
                    : "";

            System.out.printf("  %s %s %s - %s%n",
                    Ui.BOLD + log.ticketNumber() + Ui.RESET,
                    ui.statusBadge("CLOSED"),
                    reassignTag,
                    Ui.BOLD + ui.truncate(log.title(), 55) + Ui.RESET);

            String closedDate = log.closedAt() != null ? ui.formatDateTime(log.closedAt()) : "N/A";
            System.out.printf("    Category : %s | Closed by: %s | Date: %s%n",
                    log.category() != null ? log.category() : "General",
                    log.closedBy() != null ? log.closedBy() : "Admin",
                    closedDate);

            System.out.printf("    Agent    : %s%n", log.assignedAgentName() != null ? log.assignedAgentName() : "Unassigned");
            if (log.closureNote() != null && !log.closureNote().isBlank()) {
                System.out.printf("    Closure  : \"%s\"%n", log.closureNote());
            }
        }
    }

    /**
     * [M] Comment workflow for admin:
     * Delegates to shared CommentFlow.
     */
    private boolean handleComment(String authHeader) {
        return commentFlow.run(authHeader, false);
    }

    /**
     * [I] Incident Tickets workflow:
     * 1. Ask for incident number (e.g. INC-2, 2, inc-2). b = cancel.
     * 2. Spinner -> show incident in a box (number, title, location, colored severity with points, description, counts).
     * 3. List linked tickets sorted by urgency highest first.
     * 4. Ask: "Enter a ticket number to open its details, or b to go back: " -> opens ticket details screen.
     * 5. Errors: print red message, no "Press Enter".
     */
    private void handleIncidentTickets(String authHeader) {
        String input = ui.readLine("Enter incident number (e.g. INC-2): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return;
        }

        Integer incId = parseIncidentId(input);
        if (incId == null) {
            ui.printError("Invalid incident number. Please enter in format INC-2 or 2.");
            return;
        }

        IncidentResponse inc;
        List<IncidentTicketResponse> tickets;
        try {
            inc = ui.showSpinner("Loading incident...", () -> apiClient.getIncident(incId, authHeader));
            tickets = ui.showSpinner("Loading linked tickets...", () -> apiClient.getIncidentTickets(incId, authHeader));
        } catch (NoSuchElementException e) {
            ui.printError("Incident INC-" + incId + " not found.");
            return;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Error loading incident.");
            return;
        }

        List<IncidentTicketResponse> sortedTickets = new ArrayList<>(tickets);
        sortedTickets.sort((a, b) -> Integer.compare(b.urgencyScore(), a.urgencyScore()));

        int totalCount = sortedTickets.size();
        long openCount = sortedTickets.stream()
                .filter(t -> !"RESOLVED".equalsIgnoreCase(t.status()) && !"CLOSED".equalsIgnoreCase(t.status()))
                .count();

        System.out.println();
        ui.printBoxHeader("INCIDENT: INC-" + inc.incidentId());
        System.out.printf("  Number     : INC-%d%n", inc.incidentId());
        System.out.printf("  Title      : %s%n", inc.title());
        System.out.printf("  Location   : %s%n", inc.location() != null ? inc.location() : "Campus Wide");
        System.out.printf("  Severity   : %s%n", formatSeverityWithPoints(inc.severity()));
        System.out.printf("  Counts     : Tickets: %d | Open: %d%n", totalCount, openCount);
        System.out.println();
        System.out.println("  Description:");
        if (inc.description() != null && !inc.description().isBlank()) {
            for (String line : inc.description().split("\n")) {
                System.out.println("    " + line);
            }
        } else {
            System.out.println("    (No description provided)");
        }

        ui.printBoxHeader("LINKED TICKETS (" + totalCount + ")");
        if (sortedTickets.isEmpty()) {
            System.out.println("  No tickets linked to this incident yet.");
        } else {
            for (IncidentTicketResponse t : sortedTickets) {
                String overdueTag = ui.overdueTag(t.overdue(), t.status());
                String ticketNum = Ui.BOLD + (t.ticketNumber() != null ? t.ticketNumber() : "HD-" + t.ticketId()) + Ui.RESET;
                String status = ui.statusBadge(t.status());
                String urgency = ui.urgencyBadge(t.urgencyLevel(), t.urgencyScore());
                String agent = t.assignedAgentName() != null ? t.assignedAgentName() : (t.assignedAgent() != null ? t.assignedAgent() : "Unassigned");

                System.out.printf("  %s%s  %s  %s  Agent: %s%n", overdueTag, ticketNum, status, urgency, agent);
                if (t.title() != null && !t.title().isBlank()) {
                    System.out.println("    " + Ui.BOLD + ui.truncate(t.title(), 68) + Ui.RESET);
                }
            }
        }
        ui.printBoxFooter();

        while (true) {
            String ticketChoice = ui.readLine("Enter a ticket number to open its details, or b to go back: ").trim();
            if (ticketChoice.equalsIgnoreCase("b") || ticketChoice.isEmpty()) {
                return;
            }
            showTicketDetails(ticketChoice, authHeader);
        }
    }

    /**
     * [C] Close Ticket workflow:
     * 1. Ask "Enter ticket number to close (e.g. HD-1042):". Accept 1042, hd-1042, HD-1042. b = cancel.
     * 2. Spinner, then a short box: number, title, colored status.
     * 3. If ticket is not RESOLVED, show red error ("Only RESOLVED tickets can be closed. This ticket is ...") and return.
     * 4. Ask for closure note (10-500 characters, trimmed). Re-ask only that field if invalid.
     * 5. Ask "Close HD-XXXX? (Y/n)". Spinner. Green "Ticket HD-XXXX closed."
     */
    private boolean handleCloseTicket(String authHeader) {
        String input = ui.readLine("Enter ticket number to close (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);
        TicketDetailResponse ticket;
        try {
            ticket = ui.showSpinner("Loading ticket...", () -> apiClient.getTicketDetail(ticketNumber, authHeader));
        } catch (NoSuchElementException e) {
            ui.printError("Ticket " + ticketNumber + " not found, or you don't have access to it.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Error loading ticket " + ticketNumber);
            return false;
        }

        return closeTicketFlow(ticket, authHeader);
    }

    private boolean closeTicketFlow(TicketDetailResponse ticket, String authHeader) {
        System.out.println();
        ui.printBoxHeader("TICKET: " + ticket.ticketNumber());
        System.out.printf("  Number: %s%n", ticket.ticketNumber());
        System.out.printf("  Title : %s%n", ticket.title());
        System.out.printf("  Status: %s%n", ui.statusBadge(ticket.status()));
        ui.printBoxFooter();

        if (!"RESOLVED".equalsIgnoreCase(ticket.status())) {
            ui.printError("Only RESOLVED tickets can be closed. This ticket is " + ticket.status() + ".");
            return false;
        }

        String note = null;
        while (note == null) {
            String noteInput = ui.readLine("Enter closure note (10-500 characters, b to cancel): ").trim();
            if (noteInput.equalsIgnoreCase("b")) {
                return false;
            }
            if (noteInput.length() < 10 || noteInput.length() > 500) {
                ui.printError("Closure note must be between 10 and 500 characters (got " + noteInput.length() + ").");
            } else {
                note = noteInput;
            }
        }

        String confirm = ui.readLine("Close " + ticket.ticketNumber() + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        final String finalNote = note;
        try {
            ui.showSpinner("Closing ticket " + ticket.ticketNumber() + "...", () -> {
                apiClient.closeTicket(ticket.ticketId(), finalNote, authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticket.ticketNumber() + " closed.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to close ticket.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to close ticket: " + e.getMessage());
            return false;
        }
    }

    /**
     * [O] Reopen Ticket workflow:
     * 1. Ask "Enter ticket number to reopen (e.g. HD-1042):". Accept 1042, hd-1042, HD-1042. b = cancel.
     * 2. Spinner, then a short box: number, title, colored status.
     * 3. Status rule: Only RESOLVED tickets can be reopened. If CLOSED: "Closed tickets cannot be changed."
     *    Otherwise: "Only RESOLVED tickets can be reopened. This ticket is ..."
     * 4. Show status target: "HD-XXXX will move to IN_PROGRESS."
     * 5. Ask for reopen reason (10-500 characters, trimmed). Re-ask only that field if invalid.
     * 6. Ask "Reopen HD-XXXX to IN_PROGRESS? (Y/n)". Spinner. Green "Ticket HD-XXXX reopened to IN_PROGRESS."
     */
    private boolean handleReopenTicket(String authHeader) {
        String input = ui.readLine("Enter ticket number to reopen (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);
        TicketDetailResponse ticket;
        try {
            ticket = ui.showSpinner("Loading ticket...", () -> apiClient.getTicketDetail(ticketNumber, authHeader));
        } catch (NoSuchElementException e) {
            ui.printError("Ticket " + ticketNumber + " not found, or you don't have access to it.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Error loading ticket " + ticketNumber);
            return false;
        }

        return reopenTicketFlow(ticket, authHeader);
    }

    private boolean reopenTicketFlow(TicketDetailResponse ticket, String authHeader) {
        System.out.println();
        ui.printBoxHeader("TICKET: " + ticket.ticketNumber());
        System.out.printf("  Number: %s%n", ticket.ticketNumber());
        System.out.printf("  Title : %s%n", ticket.title());
        System.out.printf("  Status: %s%n", ui.statusBadge(ticket.status()));
        ui.printBoxFooter();

        if ("CLOSED".equalsIgnoreCase(ticket.status())) {
            ui.printError("Closed tickets cannot be changed.");
            return false;
        }

        if (!"RESOLVED".equalsIgnoreCase(ticket.status())) {
            ui.printError("Only RESOLVED tickets can be reopened. This ticket is " + ticket.status() + ".");
            return false;
        }

        System.out.println("  " + Ui.CYAN + ticket.ticketNumber() + " will move to IN_PROGRESS." + Ui.RESET);

        String reason = null;
        while (reason == null) {
            String reasonInput = ui.readLine("Enter reopen reason (10-500 characters, b to cancel): ").trim();
            if (reasonInput.equalsIgnoreCase("b")) {
                return false;
            }
            if (reasonInput.length() < 10 || reasonInput.length() > 500) {
                ui.printError("Reopen reason must be between 10 and 500 characters (got " + reasonInput.length() + ").");
            } else {
                reason = reasonInput;
            }
        }

        String confirm = ui.readLine("Reopen " + ticket.ticketNumber() + " to IN_PROGRESS? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        final String finalReason = reason;
        try {
            ui.showSpinner("Reopening ticket " + ticket.ticketNumber() + "...", () -> {
                apiClient.reopenTicket(ticket.ticketId(), finalReason, authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticket.ticketNumber() + " reopened to IN_PROGRESS.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to reopen ticket.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to reopen ticket: " + e.getMessage());
            return false;
        }
    }

    /**
     * [N] Declare Incident workflow:
     * Title (non-empty), Location (non-empty), Severity (1-4, Enter = MEDIUM), Description (non-empty).
     */
    private boolean handleDeclareIncident(String authHeader) {
        ui.printSubHeader("DECLARE CAMPUS INCIDENT");

        String title = null;
        while (title == null) {
            String t = ui.readLine("Incident Title (b to cancel): ").trim();
            if (t.equalsIgnoreCase("b")) return false;
            if (t.isBlank()) {
                ui.printError("Incident title cannot be blank.");
            } else {
                title = t;
            }
        }

        String location = null;
        while (location == null) {
            String loc = ui.readLine("Location (e.g. Science Block, b to cancel): ").trim();
            if (loc.equalsIgnoreCase("b")) return false;
            if (loc.isBlank()) {
                ui.printError("Location cannot be blank.");
            } else {
                location = loc;
            }
        }

        System.out.println(Ui.BOLD + "Severity Level:" + Ui.RESET);
        System.out.println("  1 LOW (+5 pts)");
        System.out.println("  2 MEDIUM (+10 pts, Default)");
        System.out.println("  3 HIGH (+20 pts)");
        System.out.println("  4 CRITICAL (+30 pts)");
        String severity = null;
        while (severity == null) {
            String sevInput = ui.readLine("Pick severity (1-4) [Enter for MEDIUM, b to cancel]: ").trim();
            if (sevInput.equalsIgnoreCase("b")) return false;
            if (sevInput.isEmpty() || sevInput.equals("2") || sevInput.equalsIgnoreCase("medium")) {
                severity = "MEDIUM";
            } else if (sevInput.equals("1") || sevInput.equalsIgnoreCase("low")) {
                severity = "LOW";
            } else if (sevInput.equals("3") || sevInput.equalsIgnoreCase("high")) {
                severity = "HIGH";
            } else if (sevInput.equals("4") || sevInput.equalsIgnoreCase("critical")) {
                severity = "CRITICAL";
            } else {
                ui.printError("Please enter 1-4, or press Enter for MEDIUM.");
            }
        }

        String description = null;
        while (description == null) {
            String desc = ui.readLine("Description of scope & impact (b to cancel): ").trim();
            if (desc.equalsIgnoreCase("b")) return false;
            if (desc.isBlank()) {
                ui.printError("Description cannot be blank.");
            } else {
                description = desc;
            }
        }

        System.out.println();
        ui.printSubHeader("INCIDENT SUMMARY");
        System.out.println("  Title      : " + title);
        System.out.println("  Location   : " + location);
        System.out.println("  Severity   : " + severity);
        System.out.println("  Description: " + description);
        ui.printDivider();

        String confirm = ui.readLine("Declare this incident? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        final String fTitle = title;
        final String fLoc = location;
        final String fSev = severity;
        final String fDesc = description;
        try {
            IncidentResponse inc = ui.showSpinner("Declaring incident...", () ->
                    apiClient.createIncident(fTitle, fLoc, fSev, fDesc, authHeader)
            );
            ui.printSuccess("Incident INC-" + inc.incidentId() + " declared.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to declare incident.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to declare incident: " + e.getMessage());
            return false;
        }
    }

    /**
     * [L] Link Ticket to Incident workflow:
     * Shows active incidents, asks for incident number, ticket number, confirms and links.
     */
    private boolean handleLinkTicket(AdminDataHolder data, String authHeader) {
        ui.printSubHeader("LINK TICKET TO CAMPUS INCIDENT");

        List<IncidentResponse> activeIncidents = data.incidents;
        if (activeIncidents == null || activeIncidents.isEmpty()) {
            ui.printError("No active incidents to link tickets to. Declare an incident first.");
            return false;
        }

        System.out.println(Ui.BOLD + "Active Incidents:" + Ui.RESET);
        for (IncidentResponse inc : activeIncidents) {
            System.out.printf("  INC-%d [%s] - %s%n", inc.incidentId(), inc.severity(), inc.title());
        }
        ui.printDivider();

        String incInput = ui.readLine("Enter incident number (e.g. INC-2, b to cancel): ").trim();
        if (incInput.equalsIgnoreCase("b") || incInput.isEmpty()) {
            return false;
        }

        Integer incId = parseIncidentId(incInput);
        if (incId == null) {
            ui.printError("Invalid incident number. Please enter in format INC-2 or 2.");
            return false;
        }

        IncidentResponse inc;
        try {
            inc = ui.showSpinner("Validating incident...", () -> apiClient.getIncident(incId, authHeader));
        } catch (NoSuchElementException e) {
            ui.printError("Incident INC-" + incId + " not found.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Error loading incident.");
            return false;
        }

        String ticketInput = ui.readLine("Enter ticket number to link (e.g. HD-1042, b to cancel): ").trim();
        if (ticketInput.equalsIgnoreCase("b") || ticketInput.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(ticketInput);
        TicketDetailResponse ticket;
        try {
            ticket = ui.showSpinner("Validating ticket...", () -> apiClient.getTicketDetail(ticketNumber, authHeader));
        } catch (NoSuchElementException e) {
            ui.printError("Ticket " + ticketNumber + " not found.");
            return false;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Error loading ticket.");
            return false;
        }

        if ("CLOSED".equalsIgnoreCase(ticket.status())) {
            ui.printError("CLOSED tickets cannot be modified or linked to incidents.");
            return false;
        }

        System.out.println();
        ui.printBoxHeader("LINK CONFIRMATION");
        System.out.printf("  Incident : INC-%d - %s%n", inc.incidentId(), inc.title());
        System.out.printf("  Ticket   : %s - %s%n", ticket.ticketNumber(), ticket.title());
        ui.printBoxFooter();

        String confirm = ui.readLine("Link " + ticket.ticketNumber() + " to INC-" + inc.incidentId() + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        try {
            ui.showSpinner("Linking ticket...", () -> {
                apiClient.linkTicketToIncident(inc.incidentId(), ticket.ticketId(), authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticket.ticketNumber() + " linked to INC-" + inc.incidentId() + ".");
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to link ticket.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to link ticket: " + e.getMessage());
            return false;
        }
    }

    /**
     * [A] Add Agent workflow:
     * Name, email, temporary password (min 6, masked).
     */
    private boolean handleAddAgent(String authHeader) {
        ui.printSubHeader("ADD SUPPORT AGENT");

        String name = null;
        while (name == null) {
            String n = ui.readLine("Agent Full Name (b to cancel): ").trim();
            if (n.equalsIgnoreCase("b")) return false;
            if (n.length() < 2) {
                ui.printError("Full name must be at least 2 characters.");
            } else {
                name = n;
            }
        }

        String email = null;
        while (email == null) {
            String em = ui.readLine("Email Address (b to cancel): ").trim();
            if (em.equalsIgnoreCase("b")) return false;
            if (!em.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                ui.printError("Enter a valid email address.");
            } else {
                email = em;
            }
        }

        String password = null;
        while (password == null) {
            String pwd = ui.readPassword("Temporary Password (min 6 chars, b to cancel): ").trim();
            if (pwd.equalsIgnoreCase("b")) return false;
            if (pwd.length() < 6) {
                ui.printError("Password must be at least 6 characters.");
            } else {
                password = pwd;
            }
        }

        System.out.println();
        ui.printSubHeader("NEW SUPPORT AGENT SUMMARY");
        System.out.println("  Name : " + name);
        System.out.println("  Email: " + email);
        System.out.println("  Role : AGENT");
        ui.printDivider();

        String confirm = ui.readLine("Create support agent account? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        final String fName = name;
        final String fEmail = email;
        final String fPassword = password;
        try {
            UserResponse agent = ui.showSpinner("Creating agent account...", () ->
                    apiClient.createAgent(fName, fEmail, fPassword, authHeader)
            );
            ui.printSuccess("Support agent " + agent.name() + " (" + agent.email() + ") created.");
            return true;
        } catch (IllegalArgumentException e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to create agent.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Failed to create agent: " + e.getMessage());
            return false;
        }
    }

    /**
     * T: View Ticket Details modal for admins.
     */
    private boolean handleViewTicket(String authHeader) {
        String input = ui.readLine("Enter ticket number (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }
        return showTicketDetails(input, authHeader);
    }

    /**
     * Shared ticket details viewer for [T], [I], and status workflows.
     */
    private boolean showTicketDetails(String rawInput, String authHeader) {
        String ticketNumber = normalizeTicketNumber(rawInput);

        try {
            TicketDetailResponse ticket = ui.showSpinner("Loading ticket details...", () ->
                    apiClient.getTicketDetail(ticketNumber, authHeader)
            );

            List<AssignmentRecord> history = apiClient.getAssignmentHistory(ticket.ticketId(), authHeader);

            renderAdminTicketDetailView(ticket, history);

            // Present valid status/assignment actions based on current status
            String st = ticket.status() != null ? ticket.status().toUpperCase() : "OPEN";
            List<String> actionOptions = new ArrayList<>();
            if ("ASSIGNED".equals(st)) {
                actionOptions.add("[S] Start Work");
                actionOptions.add("[A] Reassign Agent");
            } else if ("IN_PROGRESS".equals(st)) {
                actionOptions.add("[D] Done (Resolve)");
                actionOptions.add("[A] Reassign Agent");
            } else if ("OPEN".equals(st)) {
                actionOptions.add("[A] Assign Agent");
            } else if ("RESOLVED".equals(st)) {
                actionOptions.add("[C] Close Ticket");
                actionOptions.add("[O] Reopen Ticket");
            }

            if (!actionOptions.isEmpty()) {
                System.out.println("  Actions: " + String.join(" | ", actionOptions) + " | [Enter] Go back");
                String choice = ui.readLine("Choose action (or press Enter to return): ").trim().toLowerCase();
                if (choice.equals("s") || choice.equals("start")) {
                    if ("ASSIGNED".equals(st)) {
                        return handleDirectStartWork(ticket, authHeader);
                    }
                } else if (choice.equals("d") || choice.equals("done") || choice.equals("resolve")) {
                    if ("IN_PROGRESS".equals(st)) {
                        return handleDirectResolve(ticket, authHeader);
                    }
                } else if (choice.equals("a") || choice.equals("assign") || choice.equals("reassign")) {
                    if (!"CLOSED".equals(st) && !"RESOLVED".equals(st)) {
                        return reassignAgentFlow(ticket, authHeader);
                    }
                } else if (choice.equals("c") || choice.equals("close")) {
                    if ("RESOLVED".equals(st)) {
                        return closeTicketFlow(ticket, authHeader);
                    }
                } else if (choice.equals("o") || choice.equals("reopen")) {
                    if ("RESOLVED".equals(st)) {
                        return reopenTicketFlow(ticket, authHeader);
                    }
                }
            } else {
                ui.readLine("Press Enter to go back...");
            }
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

    private boolean handleDirectStartWork(TicketDetailResponse ticket, String authHeader) {
        String confirm = ui.readLine("Start work on " + ticket.ticketNumber() + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }
        try {
            ui.showSpinner("Starting work...", () -> {
                apiClient.updateTicketStatus(ticket.ticketId(), "IN_PROGRESS", authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticket.ticketNumber() + " is now IN_PROGRESS.");
            return true;
        } catch (Exception e) {
            ui.printError("Failed to update status: " + e.getMessage());
            return false;
        }
    }

    private boolean handleDirectResolve(TicketDetailResponse ticket, String authHeader) {
        String confirm = ui.readLine("Resolve ticket " + ticket.ticketNumber() + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }
        try {
            ui.showSpinner("Resolving ticket...", () -> {
                apiClient.updateTicketStatus(ticket.ticketId(), "RESOLVED", authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticket.ticketNumber() + " marked as RESOLVED.");
            return true;
        } catch (Exception e) {
            ui.printError("Failed to resolve ticket: " + e.getMessage());
            return false;
        }
    }

    private boolean reassignAgentFlow(TicketDetailResponse ticket, String authHeader) {
        List<UserResponse> agents;
        try {
            agents = ui.showSpinner("Loading agents...", () -> apiClient.getAgents(authHeader));
        } catch (Exception e) {
            ui.printError("Failed to load agents: " + e.getMessage());
            return false;
        }

        List<UserResponse> candidates = agents.stream()
                .filter(a -> ticket.assignedTo() == null || !ticket.assignedTo().equals(a.userId()))
                .toList();

        if (candidates.isEmpty()) {
            ui.printError("No other agents available to assign.");
            return false;
        }

        System.out.println(Ui.BOLD + "Available Agents:" + Ui.RESET);
        for (int i = 0; i < candidates.size(); i++) {
            UserResponse ag = candidates.get(i);
            System.out.printf("  %d. %s (%s)%n", (i + 1), ag.name(), ag.email());
        }

        UserResponse selectedAgent = null;
        while (selectedAgent == null) {
            String choice = ui.readLine("Select agent number (1-" + candidates.size() + ", b to cancel): ").trim();
            if (choice.equalsIgnoreCase("b")) return false;
            try {
                int idx = Integer.parseInt(choice);
                if (idx >= 1 && idx <= candidates.size()) {
                    selectedAgent = candidates.get(idx - 1);
                } else {
                    ui.printError("Please enter a number between 1 and " + candidates.size() + ".");
                }
            } catch (NumberFormatException e) {
                ui.printError("Please enter a valid number.");
            }
        }

        String reason = null;
        while (reason == null) {
            String r = ui.readLine("Enter reassignment reason (10-500 characters, b to cancel): ").trim();
            if (r.equalsIgnoreCase("b")) return false;
            if (r.length() < 10 || r.length() > 500) {
                ui.printError("Reassignment reason must be between 10 and 500 characters (got " + r.length() + ").");
            } else {
                reason = r;
            }
        }

        String actionName = ticket.assignedTo() != null ? "Reassign" : "Assign";
        String confirm = ui.readLine(actionName + " " + ticket.ticketNumber() + " to " + selectedAgent.name() + "? (Y/n) ").trim().toLowerCase();
        if (confirm.equals("n") || confirm.equals("no") || confirm.equals("b")) {
            return false;
        }

        final UserResponse fAgent = selectedAgent;
        final String fReason = reason;
        try {
            ui.showSpinner("Assigning agent...", () -> {
                apiClient.assignTicket(ticket.ticketId(), fAgent.userId(), fReason, authHeader);
                return null;
            });
            ui.printSuccess("Ticket " + ticket.ticketNumber() + " assigned to " + fAgent.name() + ".");
            return true;
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to assign ticket.");
            return false;
        }
    }

    private void renderAdminTicketDetailView(TicketDetailResponse ticket, List<AssignmentRecord> history) {
        System.out.println();
        ui.printBoxHeader("TICKET DETAILS: " + ticket.ticketNumber());
        System.out.printf("  Ticket Number     : %s%n", ticket.ticketNumber());
        System.out.printf("  Title             : %s%n", ticket.title());
        System.out.printf("  Status            : %s%n", ui.statusBadge(ticket.status()));
        System.out.printf("  Category          : %s%n", ticket.categoryName() != null ? ticket.categoryName() : "General");
        System.out.printf("  Creator           : %s%n", ticket.creatorName() != null ? ticket.creatorName() : "Student");
        System.out.printf("  Assigned Agent    : %s%n", ticket.assignedAgentName() != null ? ticket.assignedAgentName() : "Not assigned yet");
        System.out.printf("  Urgency Rating    : %s%n", ui.urgencyBadge(ticket.urgencyLevel(), ticket.urgencyScore()));
        System.out.printf("  Requested Priority: %s%n", ticket.requestedPriority() != null ? ticket.requestedPriority() : "LOW");

        String dueAtStr = ticket.dueAt() != null ? ui.formatDateTime(ticket.dueAt()) : "None";
        System.out.printf("  SLA Deadline      : %s%n", dueAtStr);

        String overdueTag = ui.overdueTag(ticket.overdue(), ticket.status());
        System.out.printf("  SLA / Overdue     : %s%n", overdueTag.isEmpty() ? "On Schedule" : overdueTag.trim());

        if (ticket.incidentTitle() != null && !ticket.incidentTitle().isBlank()) {
            System.out.printf("  Linked Incident   : %s%n", ticket.incidentTitle());
        }

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

        // Urgency Analysis Factors Section
        ui.printBoxHeader("URGENCY ANALYSIS FACTORS");
        List<String> reasons = ticket.reasons();
        if (reasons == null || reasons.isEmpty()) {
            System.out.println("  No specific urgency factors triggered.");
        } else {
            for (String reason : reasons) {
                System.out.println("  * " + reason);
            }
        }

        // Assignment History Section (Admin Only)
        ui.printBoxHeader("ASSIGNMENT HISTORY");
        if (history == null || history.isEmpty()) {
            System.out.println("  No assignment history.");
        } else {
            for (AssignmentRecord record : history) {
                String from = record.fromAgentName() != null ? record.fromAgentName() : "None";
                String to = record.toAgentName() != null ? record.toAgentName() : "Unassigned";
                String by = record.changedByName() != null ? record.changedByName() : "System";
                String time = ui.formatDateTime(record.changedAt());
                String reason = record.reason() != null && !record.reason().isBlank() ? " | Reason: " + record.reason() : "";
                System.out.printf("  [%s] %s -> %s (by %s)%s%n", time, from, to, by, reason);
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

    private Integer parseIncidentId(String input) {
        if (input == null) return null;
        String clean = input.trim().toUpperCase();
        if (clean.equalsIgnoreCase("b") || clean.isEmpty()) {
            return null;
        }
        if (clean.startsWith("INC-")) {
            clean = clean.substring(4);
        } else if (clean.startsWith("INC")) {
            clean = clean.substring(3);
        }
        try {
            return Integer.parseInt(clean.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String formatSeverityWithPoints(String severity) {
        if (severity == null) severity = "LOW";
        String sev = severity.toUpperCase();
        return switch (sev) {
            case "CRITICAL" -> Ui.BOLD_RED + "CRITICAL (+30 pts)" + Ui.RESET;
            case "HIGH" -> Ui.LIGHT_RED + "HIGH (+20 pts)" + Ui.RESET;
            case "MEDIUM" -> Ui.YELLOW + "MEDIUM (+10 pts)" + Ui.RESET;
            case "LOW" -> Ui.DIM + "LOW (+5 pts)" + Ui.RESET;
            default -> sev;
        };
    }

    @Override
    protected void printControls() {
        ui.printDoubleDivider();
        System.out.println("  [1-7] Tabs | [T] Ticket Details | [M] Comment | [I] Incident Tickets");
        System.out.println("  [C] Close Ticket | [O] Reopen Ticket | [N] Declare Incident");
        System.out.println("  [A] Add Agent | [L] Link Ticket | [R] Refresh | [Q] Logout");
        ui.printDoubleDivider();
    }

    private String renderBar(int value, int max, int barLength) {
        if (max <= 0) return "[" + " ".repeat(barLength) + "]";
        int filled = (int) Math.round(((double) value / max) * barLength);
        if (filled > barLength) filled = barLength;
        return "[" + "=".repeat(filled) + " ".repeat(barLength - filled) + "]";
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "AD";
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
