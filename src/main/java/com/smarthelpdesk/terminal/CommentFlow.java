package com.smarthelpdesk.terminal;

import com.smarthelpdesk.dto.CommentDetailResponse;
import com.smarthelpdesk.dto.TicketDetailResponse;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Shared comment flow implementation across Student, Agent, and Admin screens.
 */
public class CommentFlow {

    private final ApiClient apiClient;
    private final Ui ui;

    public CommentFlow(ApiClient apiClient, Ui ui) {
        this.apiClient = apiClient;
        this.ui = ui;
    }

    /**
     * Executes the comment flow.
     *
     * @param authHeader     authorization header
     * @param allowIssueNote true if the screen allows marking comments as issue notes (Student only)
     * @return true if at least one comment was posted (triggers list reload), false otherwise
     */
    public boolean run(String authHeader, boolean allowIssueNote) {
        String input = ui.readLine("Enter ticket number (e.g. HD-1042, b to cancel): ").trim();
        if (input.equalsIgnoreCase("b") || input.isEmpty()) {
            return false;
        }

        String ticketNumber = normalizeTicketNumber(input);

        TicketDetailResponse ticket;
        try {
            ticket = ui.showSpinner("Loading comments...", () ->
                    apiClient.getTicketDetail(ticketNumber, authHeader)
            );
        } catch (NoSuchElementException e) {
            ui.printError("Ticket not found, or you don't have access to it.");
            return false;
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server.");
            return false;
        } catch (Exception e) {
            ui.printError("Ticket not found, or you don't have access to it.");
            return false;
        }

        // ALWAYS show ticket's number, title, colored status, and full comment history
        renderCommentsTimeline(ticket);

        // Check ticket status: if CLOSED, show message and prompt to go back
        if ("CLOSED".equalsIgnoreCase(ticket.status())) {
            ui.printError("This ticket is CLOSED. No new comments can be added.");
            ui.readLine("Press Enter to go back...");
            return false;
        }

        boolean anyCommentPosted = false;

        while (true) {
            String commentText = ui.readLine("Type a comment and press Enter, or type x to exit: ").trim();
            if (commentText.equalsIgnoreCase("x")) {
                return anyCommentPosted;
            }

            if (commentText.isEmpty()) {
                ui.printError("Comment text cannot be blank.");
                continue;
            }

            if (commentText.length() > 1000) {
                ui.printError("Comment text cannot exceed 1000 characters.");
                continue;
            }

            String commentType = "GENERAL";
            if (allowIssueNote) {
                String markIssue = ui.readLine("Mark as issue note? (y/N): ").trim().toLowerCase();
                if (markIssue.equals("y") || markIssue.equals("yes")) {
                    commentType = "ISSUE";
                }
            }

            final String typeToSend = commentType;
            final Integer ticketId = ticket.ticketId();

            try {
                ui.showSpinner("Posting comment...", () -> {
                    apiClient.addComment(ticketId, commentText, typeToSend, authHeader);
                    return null;
                });
                ui.printSuccess("Comment posted.");
                anyCommentPosted = true;

                // Reload and show comments again
                ticket = ui.showSpinner("Reloading comments...", () ->
                        apiClient.getTicketDetail(ticketNumber, authHeader)
                );
                renderCommentsTimeline(ticket);
            } catch (IllegalArgumentException e) {
                ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to post comment.");
            } catch (IllegalStateException e) {
                ui.printError("Cannot reach the server.");
            } catch (Exception e) {
                ui.printError(e.getMessage() != null ? e.getMessage() : "Failed to post comment.");
            }
        }
    }

    /**
     * Normalizes ticket number input (accepts 1042, hd-1042, HD-1042).
     */
    public static String normalizeTicketNumber(String input) {
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
     * Renders ticket's number, title, colored status, and full comment history oldest first.
     */
    public void renderCommentsTimeline(TicketDetailResponse ticket) {
        System.out.println();
        String titleSnip = ticket.title() != null ? ui.truncate(ticket.title(), 45) : "";
        ui.printBoxHeader("COMMENTS: " + ticket.ticketNumber() + " - " + titleSnip);
        System.out.printf("  Ticket Number: %s%n", ticket.ticketNumber());
        System.out.printf("  Title        : %s%n", ticket.title() != null ? ticket.title() : "");
        System.out.printf("  Status       : %s%n", ui.statusBadge(ticket.status()));
        System.out.println();

        List<CommentDetailResponse> comments = ticket.comments();
        if (comments == null || comments.isEmpty()) {
            System.out.println("  No comments yet.");
        } else {
            String creator = ticket.creatorName() != null ? ticket.creatorName() : ticket.createdByName();
            for (int i = 0; i < comments.size(); i++) {
                CommentDetailResponse c = comments.get(i);
                String role = "User";
                if (ticket.assignedAgentName() != null && ticket.assignedAgentName().equalsIgnoreCase(c.authorName())) {
                    role = "Agent";
                } else if (creator != null && creator.equalsIgnoreCase(c.authorName())) {
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
}
