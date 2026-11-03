package com.smarthelpdesk.terminal;

/**
 * BaseScreen is an abstract class that serves as the common base for all terminal screens.
 *
 * What is an abstract class?
 * An abstract class is a restricted class that cannot be used to create objects directly
 * (you cannot do 'new BaseScreen()'). It acts as a foundation or template that child classes
 * (StudentScreen, AgentScreen, AdminScreen) inherit from. It lets child classes share common code
 * (like showControlsAndPrompt) while enforcing that every screen implements its own controls.
 */
public abstract class BaseScreen {

    /**
     * The prompt string displayed when waiting for user input on this screen.
     */
    protected String promptText;

    public BaseScreen() {
    }

    public BaseScreen(String promptText) {
        this.promptText = promptText;
    }

    /**
     * What is an abstract method?
     * An abstract method has a method signature but NO body (no curly braces {} or code).
     * It forces every child class extending BaseScreen to provide its own concrete implementation
     * using @Override. Here, each screen MUST implement printControls() to display its own
     * specific menu options.
     */
    protected abstract void printControls();

    /**
     * Shared method inherited by all screens:
     * Calls printControls() (which polymorphically executes the child screen's controls),
     * and then prints the "Enter your choice" prompt that the screen already uses.
     */
    protected void showControlsAndPrompt() {
        printControls();
        if (promptText != null) {
            System.out.print(promptText);
        }
    }

    /**
     * Overloaded variant that prints controls and a custom prompt text.
     */
    protected void showControlsAndPrompt(String customPrompt) {
        printControls();
        if (customPrompt != null) {
            System.out.print(customPrompt);
        }
    }
}
