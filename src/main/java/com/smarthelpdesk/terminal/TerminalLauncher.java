package com.smarthelpdesk.terminal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Entry hook that asks "Run terminal UI? (y/N)" and launches the terminal application.
 * Active only when app.terminal.ask=true.
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "app.terminal.ask", havingValue = "true")
public class TerminalLauncher implements ApplicationRunner {

    private final LoginScreen loginScreen;
    private final StudentScreen studentScreen;
    private final AgentScreen agentScreen;
    private final Ui ui;

    public TerminalLauncher(LoginScreen loginScreen, StudentScreen studentScreen, AgentScreen agentScreen, Ui ui) {
        this.loginScreen = loginScreen;
        this.studentScreen = studentScreen;
        this.agentScreen = agentScreen;
        this.ui = ui;
    }

    @Override
    public void run(ApplicationArguments args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Run terminal UI? (y/N) ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes")) {
                startSession();
            } else if (input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no")) {
                System.out.println("Backend continues running.");
                break;
            } else {
                System.out.println("Invalid input. Please enter 'y' or 'n'.");
            }
        }
    }

    private void startSession() {
        while (true) {
            TerminalSession session = loginScreen.show();
            if (session == null) {
                // User exited from login screen
                break;
            }

            if ("STUDENT".equalsIgnoreCase(session.getRole())) {
                studentScreen.show(session);
                // When student logs out (Q), return to ask "Run terminal UI? (y/N)"
                break;
            } else if ("AGENT".equalsIgnoreCase(session.getRole())) {
                agentScreen.show(session);
                // When agent logs out (Q), return to ask "Run terminal UI? (y/N)"
                break;
            } else if ("ADMIN".equalsIgnoreCase(session.getRole())) {
                System.out.println("Admin dashboard coming in the next step");
                // Returns to login screen loop
            } else {
                ui.printError("Unknown role: " + session.getRole());
            }
        }
    }
}
