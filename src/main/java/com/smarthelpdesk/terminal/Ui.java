package com.smarthelpdesk.terminal;

import org.springframework.stereotype.Component;

import java.io.Console;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Terminal UI helper for ANSI colors, badges, boxes, inputs, and animated spinner.
 */
@Component
public class Ui {

    // ANSI Text Styles & Colors
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String GRAY = "\u001B[90m";
    public static final String LIGHT_RED = "\u001B[91m";
    public static final String BOLD_RED = "\u001B[1;31m";

    public static final int DEFAULT_WIDTH = 76;
    public static final String LINE_EQUALS = "=".repeat(DEFAULT_WIDTH);
    public static final String LINE_DASHES = "-".repeat(DEFAULT_WIDTH);

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");

    private final Scanner scanner;

    public Ui() {
        this.scanner = new Scanner(System.in);
    }

    public void printHeader(String title) {
        System.out.println(CYAN + "+" + LINE_DASHES.substring(2) + "+" + RESET);
        System.out.println(BOLD + CYAN + center(title, DEFAULT_WIDTH) + RESET);
        System.out.println(CYAN + "+" + LINE_DASHES.substring(2) + "+" + RESET);
    }

    public void printBoxHeader(String title) {
        System.out.println(CYAN + "+" + LINE_DASHES.substring(2) + "+" + RESET);
        int pad = Math.max(1, DEFAULT_WIDTH - title.length() - 4);
        System.out.println(BOLD + "| " + title + " ".repeat(pad) + " |" + RESET);
        System.out.println(CYAN + "+" + LINE_DASHES.substring(2) + "+" + RESET);
    }

    public void printBoxFooter() {
        System.out.println(CYAN + "+" + LINE_DASHES.substring(2) + "+" + RESET);
    }

    public void printSubHeader(String title) {
        System.out.println(DIM + LINE_DASHES + RESET);
        System.out.println(BOLD + center(title, DEFAULT_WIDTH) + RESET);
        System.out.println(DIM + LINE_DASHES + RESET);
    }

    public void printDivider() {
        System.out.println(GRAY + LINE_DASHES + RESET);
    }

    public void printDoubleDivider() {
        System.out.println(CYAN + LINE_EQUALS + RESET);
    }

    public void printError(String message) {
        System.out.println(BOLD_RED + "[!] " + message + RESET);
    }

    public void printSuccess(String message) {
        System.out.println(GREEN + "[✓] " + message + RESET);
    }

    public void printInfo(String message) {
        System.out.println(CYAN + "[i] " + message + RESET);
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    public String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            return chars != null ? new String(chars).trim() : "";
        } else {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return "";
            }
            return scanner.nextLine().trim();
        }
    }

    public <T> T showSpinner(String message, Callable<T> task) throws Exception {
        char[] spinnerChars = new char[]{'|', '/', '-', '\\'};
        AtomicBoolean running = new AtomicBoolean(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);

        Thread spinnerThread = new Thread(() -> {
            int i = 0;
            while (running.get()) {
                System.out.print("\r" + CYAN + "[ " + spinnerChars[i % spinnerChars.length] + " ] " + message + RESET + "   ");
                System.out.flush();
                i++;
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        spinnerThread.setDaemon(true);
        spinnerThread.start();

        try {
            T result = future.get();
            running.set(false);
            spinnerThread.interrupt();
            System.out.print("\r" + " ".repeat(message.length() + 15) + "\r");
            System.out.flush();
            return result;
        } catch (ExecutionException e) {
            running.set(false);
            spinnerThread.interrupt();
            System.out.print("\r" + " ".repeat(message.length() + 15) + "\r");
            System.out.flush();
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException | CancellationException e) {
            running.set(false);
            spinnerThread.interrupt();
            System.out.print("\r" + " ".repeat(message.length() + 15) + "\r");
            System.out.flush();
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * ANSI Status Badge per ui-spec section 6.
     */
    public String statusBadge(String status) {
        if (status == null) status = "UNKNOWN";
        return switch (status.toUpperCase()) {
            case "OPEN" -> CYAN + "[OPEN]" + RESET;
            case "ASSIGNED" -> MAGENTA + "[ASSIGNED]" + RESET;
            case "IN_PROGRESS" -> YELLOW + "[IN_PROGRESS]" + RESET;
            case "RESOLVED" -> GREEN + "[RESOLVED]" + RESET;
            case "CLOSED" -> GRAY + "[CLOSED]" + RESET;
            default -> WHITE + "[" + status + "]" + RESET;
        };
    }

    /**
     * ANSI Urgency Level & Score Badge per ui-spec section 6.
     */
    public String urgencyBadge(String level, int score) {
        if (level == null) level = "LOW";
        String label = level.toUpperCase() + " Urgency (" + score + ")";
        return switch (level.toUpperCase()) {
            case "URGENT" -> BOLD_RED + "[" + label + "]" + RESET;
            case "HIGH" -> LIGHT_RED + "[" + label + "]" + RESET;
            case "MEDIUM" -> YELLOW + "[" + label + "]" + RESET;
            default -> DIM + "[" + label + "]" + RESET;
        };
    }

    /**
     * Overdue SLA Alert tag per ui-spec section 6.
     */
    public String overdueTag(boolean overdue, String status) {
        boolean active = overdue && !"RESOLVED".equalsIgnoreCase(status) && !"CLOSED".equalsIgnoreCase(status);
        if (active) {
            return BOLD_RED + "[!] OVERDUE " + RESET;
        }
        return "";
    }

    public String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATE_FORMAT);
    }

    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DATE_TIME_FORMAT);
    }

    public String center(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text;
        int left = (width - text.length()) / 2;
        int right = width - text.length() - left;
        return " ".repeat(Math.max(0, left)) + text + " ".repeat(Math.max(0, right));
    }

    public String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        if (maxLen <= 3) return text.substring(0, maxLen);
        return text.substring(0, maxLen - 3) + "...";
    }
}
