package com.hospital.harmonia.util;

import com.hospital.harmonia.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the authenticated user during the application session (in-memory singleton).
 * In a future evolution, this could become a session object with a token/expiration
 * if the system gets a central backend (see the scalability section in the guide).
 */
public final class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private static User loggedInUser;

    private SessionManager() {
    }

    public static void login(User user) {
        loggedInUser = user;
        log.info("Session started for username={} role={}", user.getUsername(), user.getRole());
    }

    public static void logout() {
        if (loggedInUser != null) {
            log.info("Session ended for username={}", loggedInUser.getUsername());
        }
        loggedInUser = null;
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    public static boolean isLoggedIn() {
        return loggedInUser != null;
    }
}
