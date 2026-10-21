package com.smarthelpdesk.assignment;

public class AssignmentException extends RuntimeException {

    private final boolean conflict;

    public AssignmentException(String message, boolean conflict) {
        super(message);
        this.conflict = conflict;
    }

    public static AssignmentException badRequest(String message) {
        return new AssignmentException(message, false);
    }

    public static AssignmentException conflict(String message) {
        return new AssignmentException(message, true);
    }

    public boolean isConflict() {
        return conflict;
    }
}
