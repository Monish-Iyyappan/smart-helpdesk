package com.smarthelpdesk.ui;

import com.smarthelpdesk.model.User;

/**
 * Holds the currently logged-in user for the terminal session.
 */
public class Session {

    private User currentUser;

    public Session() {
    }

    public Session(User currentUser) {
        this.currentUser = currentUser;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void clear() {
        this.currentUser = null;
    }
}
