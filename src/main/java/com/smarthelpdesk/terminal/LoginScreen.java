package com.smarthelpdesk.terminal;

import com.smarthelpdesk.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Handles user authentication (Sign In, Registration, Quick-Fills).
 */
@Component
public class LoginScreen {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Ui ui;
    private final ApiClient apiClient;

    public LoginScreen(Ui ui, ApiClient apiClient) {
        this.ui = ui;
        this.apiClient = apiClient;
    }

    /**
     * Prompts for authentication. Returns active TerminalSession on success, or null on exit (Q).
     */
    public TerminalSession show() {
        while (true) {
            ui.printHeader("SMART HELPDESK SYSTEM");
            System.out.println(Ui.DIM + ui.center("Signal Desk Support Engine", Ui.DEFAULT_WIDTH) + Ui.RESET);
            ui.printDivider();
            System.out.println(Ui.BOLD + ui.center("[ SH ] LOGO", Ui.DEFAULT_WIDTH) + Ui.RESET);
            System.out.println(ui.center("Sign in to Helpdesk Portal", Ui.DEFAULT_WIDTH));
            System.out.println();
            System.out.println("  < [1] Sign In >                [2] Register");
            ui.printDivider();
            System.out.println("  [DEV QUICK-FILL]:  [3] Student  [4] Agent  [5] Admin");
            ui.printDivider();
            System.out.println("  [Q] Exit Terminal UI");
            ui.printDoubleDivider();

            String choice = ui.readLine("Select action (1-5 or Q): ").trim().toUpperCase();

            switch (choice) {
                case "1" -> {
                    TerminalSession session = handleSignIn();
                    if (session != null) return session;
                }
                case "2" -> {
                    TerminalSession session = handleRegister();
                    if (session != null) return session;
                }
                case "3" -> {
                    TerminalSession session = handleQuickLogin("student1@college.example", "Test@1234", "Student");
                    if (session != null) return session;
                }
                case "4" -> {
                    TerminalSession session = handleQuickLogin("agent1@college.example", "Test@1234", "Agent");
                    if (session != null) return session;
                }
                case "5" -> {
                    TerminalSession session = handleQuickLogin("admin@college.example", "Test@1234", "Admin");
                    if (session != null) return session;
                }
                case "Q" -> {
                    return null;
                }
                default -> ui.printError("Invalid choice. Please enter 1-5 or Q.");
            }
        }
    }

    private TerminalSession handleSignIn() {
        ui.printSubHeader("SIGN IN");
        String email = ui.readLine("Email Address: ").trim();
        String password = ui.readPassword("Password: ").trim();

        if (email.isBlank() || password.isBlank()) {
            ui.printError("Email and password are required.");
            return null;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            ui.printError("Enter a valid email address.");
            return null;
        }

        try {
            UserResponse user = ui.showSpinner("Signing in...", () -> apiClient.login(email, password));
            System.out.println(Ui.GREEN + "Welcome, " + user.name() + "!" + Ui.RESET);
            String authHeader = apiClient.createBasicAuthHeader(email, password);
            return new TerminalSession(user, authHeader);
        } catch (SecurityException e) {
            ui.printError("Login failed. Please check your email and password.");
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server. Is the backend running?");
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Login failed. Please check your email and password.");
        }
        return null;
    }

    private TerminalSession handleRegister() {
        ui.printSubHeader("REGISTER ACCOUNT");
        String name = ui.readLine("Full Name: ").trim();
        if (name.length() < 2) {
            ui.printError("Full name must be at least 2 characters.");
            return null;
        }

        String email = ui.readLine("Email Address: ").trim();
        if (email.isBlank()) {
            ui.printError("Email and password are required.");
            return null;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            ui.printError("Enter a valid email address.");
            return null;
        }

        String password = ui.readPassword("Password (min 6 chars): ").trim();
        if (password.isBlank()) {
            ui.printError("Email and password are required.");
            return null;
        }
        if (password.length() < 6) {
            ui.printError("Password must be at least 6 characters.");
            return null;
        }

        try {
            UserResponse user = ui.showSpinner("Creating account...", () -> apiClient.register(name, email, password));
            System.out.println(Ui.GREEN + "Welcome, " + user.name() + "!" + Ui.RESET);
            String authHeader = apiClient.createBasicAuthHeader(email, password);
            return new TerminalSession(user, authHeader);
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server. Is the backend running?");
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Registration failed.");
        }
        return null;
    }

    private TerminalSession handleQuickLogin(String email, String password, String label) {
        try {
            UserResponse user = ui.showSpinner("Signing in as " + label + "...", () -> apiClient.login(email, password));
            System.out.println(Ui.GREEN + "Welcome, " + user.name() + "!" + Ui.RESET);
            String authHeader = apiClient.createBasicAuthHeader(email, password);
            return new TerminalSession(user, authHeader);
        } catch (SecurityException e) {
            ui.printError("Login failed. Please check your email and password.");
        } catch (IllegalStateException e) {
            ui.printError("Cannot reach the server. Is the backend running?");
        } catch (Exception e) {
            ui.printError(e.getMessage() != null ? e.getMessage() : "Login failed. Please check your email and password.");
        }
        return null;
    }
}
